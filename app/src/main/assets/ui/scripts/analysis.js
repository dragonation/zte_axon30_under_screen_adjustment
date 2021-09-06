(() => {

    const createDataMesh  = function (records) {

        if (records.length <= 0) {
            return;
        }

        let temperatureRanges = [Infinity, -Infinity];

        for (let record of records) {
            let temperature = record[0];
            if (temperature < temperatureRanges[0]) {
                temperatureRanges[0] = temperature;
            }
            if (temperature > temperatureRanges[1]) {
                temperatureRanges[1] = temperature;
            }
        }

        temperatureRanges[0] = Math.floor(temperatureRanges[0]);
        temperatureRanges[1] = Math.ceil(temperatureRanges[1]);
        if (temperatureRanges[1] - temperatureRanges[0] < 10) {
            temperatureRanges[0] -= 5;
            temperatureRanges[1] += 5;
        }

        let temperatureStep = Math.round((temperatureRanges[1] - temperatureRanges[0]) / 10);
        temperatureRanges[0] = Math.floor(temperatureRanges[0] / temperatureStep) * temperatureStep;
        temperatureRanges[1] = Math.ceil(temperatureRanges[1] / temperatureStep) * temperatureStep;


        let points = [];
        for (let temperature = temperatureRanges[0]; 
             temperature <= temperatureRanges[1] + temperatureStep / 2; 
             temperature += temperatureStep) {
            let points2 = [];
            points.push(points2);
            for (let brightness = 0; brightness <= 1.05; brightness += 0.1) {
                points2.push({
                    "temperature": temperature,
                    "brightness": brightness
                });
            }
        }

        let raws = [];
        for (let record of records) {
            let x = Math.round((record[0] - temperatureRanges[0]) / temperatureStep);
            let y = Math.round(record[1] / 0.1);
            if (points[x] && points[x][y] && (!points[x][y].raw)) {
                points[x][y].raw = record;
                raws.push([x, y, record[2]]);
                points[x][y].value = record[2];
            }
        }

        for (let x = 0; x < points.length; ++x) {
            for (let y = 0; y < points[x].length; ++y) {
                if (points[x][y].value === undefined) {
                    let min = Infinity;
                    let value = 0;
                    for (let raw of raws) {
                        let distanceX = Math.abs(raw[0] - x);
                        let distanceY = Math.abs(raw[1] - y);
                        let distance = Math.pow(distanceX, 2) + Math.pow(distanceY, 2);
                        if (distance < min) {
                            value = raw[2];
                            min = distance;
                        }
                    }
                    points[x][y].value = value;
                }
            }
        }

        const simulate = function (count) {
            for (let x = 0; x < points.length; ++x) {
                for (let y = 0; y < points[x].length; ++y) {
                    if (!points[x][y].raw) {
                        let sum = 0;
                        let count = 0;
                        if (x > 0) { sum += points[x - 1][y].value; ++count; }
                        if (y > 0) { sum += points[x][y - 1].value; ++count; }
                        if (x < points.length - 1) { sum += points[x + 1][y].value; ++count; }
                        if (y < points[x].length - 1) { sum += points[x][y + 1].value; ++count; }
                        let value = sum / count;
                        points[x][y].value2 = value;
                    }
                }
            }
            let diff = 0;
            for (let x = 0; x < points.length; ++x) {
                for (let y = 0; y < points[x].length; ++y) {
                    if (!points[x][y].raw) {
                        let diff2 = points[x][y].value - points[x][y].value2;
                        diff += diff2 * diff2;
                        points[x][y].value = points[x][y].value2;
                    }
                }
            }
            if ((count < 100) && (diff > 0.01)) {
                simulate(count + 1);
            }
        };

        simulate(0);

        let result = [];
        for (let x = 0; x < points.length; ++x) {
            let result2 = [];
            result.push(result2);
            for (let y = 0; y < points[x].length; ++y) {
                result2.push(points[x][y].value);
            }
        }

        return {
            "temperatures": points.map((points2) => points2[0].temperature),
            "brightness": points[0].map((point) => point.brightness),
            "raws": raws,
            "points": result 
        };

    };

    let chart = null;

    const createAnalysisChart = function () {

        if (chart) { return; }

        let canvas = document.createElement("canvas");
        let context = canvas.getContext("webgl2", { 
            "antialias": true, 
            "alpha": true,
            "depth": true, 
            "preserveDrawingBuffer": true,
        });

        const scene = new THREE.Scene();

        const camera = new THREE.PerspectiveCamera(75, 1, 0.1, 1000);

        const renderer = new THREE.WebGLRenderer({
            "canvas": canvas,
            "context": context,
        });
        renderer.setPixelRatio(3);
        renderer.setSize(280, 280);
        renderer.setClearColor(0xf2f2f2);
        renderer.setClearAlpha(1);
        renderer.autoClear = true;
        renderer.gammaInput = true;
        renderer.gammaOutput = true;

        document.querySelector("#analysis-chart").appendChild(renderer.domElement);

        const controls = new THREE.OrbitControls(camera, renderer.domElement);
        controls.target.set(0, 30, 0);
        controls.enablePan = false;
        controls.maxDistance = 140;
        controls.minDistance = 120;
        controls.update();
        controls.target.set(0, 0, 0);

        let ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
        ambientLight.position.set(0, 0, 0);
        scene.add(ambientLight);

        let directionalLight = new THREE.DirectionalLight(0xffffff, 0.5);
        directionalLight.position.set(-0.7, 1, 1);
        scene.add(directionalLight);

        {
            const gridHelper = new THREE.GridHelper(100, 10, 0xbbbbbb, 0xbbbbbb);
            gridHelper.position.set(0, -50, 0);
            scene.add(gridHelper);
        }

        {
            const material = new THREE.LineBasicMaterial({ "color": 0xbbbbbb });
            const createLine = (x, z) => {
                const geometry = new THREE.BufferGeometry().setFromPoints([
                    new THREE.Vector3(x * 100 - 50, -50, z * 100 - 50),
                    new THREE.Vector3(x * 100 - 50, 50, z * 100 - 50),
                ]);
                const line = new THREE.Line(geometry, material);
                scene.add(line);
            };
            createLine(0, 0);
            createLine(0, 1);
            createLine(1, 1);
            createLine(1, 0);
        }

        {
            const createAxis = (index, color, color2, label) => {
                const material = new THREE.MeshPhongMaterial({ "color": color });
                material.emissive = new THREE.Color(color2);
                const group = new THREE.Object3D();
                group.position.set(-50, -50, -50);
                switch (index) {
                    case 0: { break; }
                    case 1: { group.rotateZ(-Math.PI / 2); break; }
                    case 2: { group.rotateX(Math.PI / 2); break; }
                    default: { break; }
                }
                scene.add(group);
                {
                    const geometry = new THREE.CylinderGeometry(0.5, 0.5, 100, 8);
                    const object = new THREE.Mesh(geometry, material);
                    object.position.set(0, 50, 0);
                    group.add(object);
                }
                {
                    const geometry = new THREE.CylinderGeometry(0, 1, 10, 8);
                    const object = new THREE.Mesh(geometry, material);
                    object.position.set(0, 100, 0);
                    group.add(object);
                }

            };
            createAxis(0, 0xff0000, 0x880000, "微调");
            createAxis(1, 0x00ff00, 0x008800, "温度");
            createAxis(2, 0x0000ff, 0x000088, "亮度");
        }

        camera.position.set(-90, -2, 78);
        camera.lookAt(0, 0, 0);

        let canceled = false;

        const frameAction = function () {
            if (canceled) { return; }
            requestAnimationFrame(frameAction);
            renderer.render(scene, camera);
        };

        frameAction();

        let dataObjects = new Set();

        chart = {
            "cancel": function () {
                for (let object of dataObjects) {
                    if (object.isMaterial) {
                        object.dispose();
                    }
                    if (object.isMesh) {
                        object.parent.remove(object);
                    }
                }
                renderer.dispose();
                document.querySelector("#analysis-chart").removeChild(renderer.domElement);
                canceled = true;
                chart = null;
            },
            "update": function (data) {

                for (let object of dataObjects) {
                    if (object.isMaterial) {
                        object.dispose();
                    }
                    if (object.isMesh && object.parent) {
                        object.parent.remove(object);
                    }
                }

                dataObjects = new Set();

                let meshes = [
                    [createDataMesh(data.map(record => [record.temperature, record.brightness, record.r])), 0xff0000, 0x880000, "r"],
                    [createDataMesh(data.map(record => [record.temperature, record.brightness, record.g])), 0x00ff00, 0x008800, "g"],
                    [createDataMesh(data.map(record => [record.temperature, record.brightness, record.b])), 0x0000ff, 0x000088, "b"],
                    [createDataMesh(data.map(record => [record.temperature, record.brightness, record.a])), 0xffbb00, 0x884400, "a"],
                ];

                for (let [mesh, color, emissive, key] of meshes) {

                    for (let record of mesh.raws) {
                        const material = new THREE.MeshPhongMaterial({ "color": color });
                        material.emissive = new THREE.Color(emissive);
                        dataObjects.add(material);
                        const geometry = new THREE.SphereGeometry(1, 8, 4);
                        const point = new THREE.Mesh(geometry, material);
                        point.position.set(record[0] / (mesh.temperatures.length - 1) * 100 - 50, 
                                           record[2] * 100 - 50,
                                           record[1] / (mesh.brightness.length - 1) * 100 - 50);
                        scene.add(point);
                        dataObjects.add(point);
                    }

                    let vertices = new Float32Array(mesh.temperatures.length * mesh.brightness.length * 4);
                    let indices = [];
                    for (let x = 0; x < mesh.temperatures.length; ++x) {
                        for (let y = 0; y < mesh.brightness.length; ++y) {
                            let base = ((x * mesh.brightness.length) + y) * 4;
                            vertices[base + 0] = 100 * (x / (mesh.temperatures.length - 1)) - 50;
                            vertices[base + 1] = (mesh.points[x][y] * 100) - 50;
                            vertices[base + 2] = 100 * (y / (mesh.brightness.length - 1)) - 50;
                            vertices[base + 3] = 1;
                        }
                    }
                    for (let x = 0; x < mesh.temperatures.length - 1; ++x) {
                        for (let y = 0; y < mesh.brightness.length - 1; ++y) {
                            let base1 = x * mesh.brightness.length + y;
                            let base2 = (x + 1) * mesh.brightness.length + y;
                            let base3 = (x + 1) * mesh.brightness.length + y + 1;
                            let base4 = (x) * mesh.brightness.length + y + 1;
                            indices.push(base3, base2, base1);
                            indices.push(base4, base3, base1);
                        }
                    }

                    {
                        let geometry = new THREE.BufferGeometry();
                        geometry.setIndex(indices);
                        geometry.setAttribute("position", new THREE.Float32BufferAttribute(vertices, 4));
                        geometry.computeVertexNormals();
                        let material = new THREE.MeshPhongMaterial({ "color": color });
                        dataObjects.add(material);
                        material.depthWrite = false;
                        material.depthTest = false;
                        material.emissive = new THREE.Color(emissive);
                        material.opacity = 0.3;
                        material.transparent = true;
                        material.side = THREE.DoubleSide;
                        let mesh = new THREE.Mesh(geometry);
                        mesh.material = material;
                        dataObjects.add(mesh);
                        scene.add(mesh);
                    }

                }

            }
        };

    };

    const refreshAnalysisChart = async function () {

        let enabled = await MinunZTEAxon30Logic.isAutofitEnabled();
        if (enabled) {
            document.querySelector("#trigger-autofit").textContent = "禁用";
        } else {
            document.querySelector("#trigger-autofit").textContent = "开启";
        }

        createAnalysisChart();

        let data = await MinunZTEAxon30Logic.listRecentAdjustments(100);

        chart.update(data.records);

    };

    const cancelAnalysisChart = async function () {

        if (!chart) {
            return;
        }

        chart.cancel();

    };

    window.MinunZTEAxon30Analysis = {
        "refreshAnalysisChart": refreshAnalysisChart,
        "cancelAnalysisChart": cancelAnalysisChart,
        "createDataMesh": createDataMesh
    };

})();