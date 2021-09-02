window.addEventListener("load", async function () {

    document.body.addEventListener("contextmenu", function (event) {
        event.preventDefault();
        event.stopPropagation();
    });

    let data = {
        "slider-r": 0,
        "slider-g": 0,
        "slider-b": 0,
        "slider-a": 0,
        "slider-notch": 0,
    };
    let sliders = Object.create(null);

    const updateNotch = function (notch) {
        if (notch < 0.1) {
            document.querySelector("#sample-mi").style["opacity"] = notch * 10;
            document.querySelector("#sample-notch").style["opacity"] = 0;
            document.querySelector("#sample-corner-left").style["width"] = `0px`;
            document.querySelector("#sample-corner-left").style["height"] = `0px`;
            document.querySelector("#sample-corner-right").style["width"] = `0px`;
            document.querySelector("#sample-corner-right").style["height"] = `0px`;
        } else if (notch > 0.65) {
            document.querySelector("#sample-mi").style["opacity"] = 0;
            document.querySelector("#sample-notch").style["opacity"] = 1;
            document.querySelector("#sample-notch").style["width"] = `${360 + 33}px`;
            document.querySelector("#sample-notch").style["margin-left"] = `-${(360 + 33) / 2}px`;
            let radius = (notch - 0.65) * 100;
            document.querySelector("#sample-corner-left").style["width"] = `${radius}px`;
            document.querySelector("#sample-corner-left").style["height"] = `${radius}px`;
            document.querySelector("#sample-corner-right").style["width"] = `${radius}px`;
            document.querySelector("#sample-corner-right").style["height"] = `${radius}px`;
        } else {
            document.querySelector("#sample-mi").style["opacity"] = 0;
            document.querySelector("#sample-notch").style["opacity"] = 1;
            document.querySelector("#sample-notch").style["width"] = `${360 * notch + 33}px`;
            document.querySelector("#sample-notch").style["margin-left"] = `-${(360 * notch + 33) / 2}px`;
            document.querySelector("#sample-corner-left").style["width"] = `0px`;
            document.querySelector("#sample-corner-left").style["height"] = `0px`;
            document.querySelector("#sample-corner-right").style["width"] = `0px`;
            document.querySelector("#sample-corner-right").style["height"] = `0px`;
        }
    };

    const reloadCurrentAdjustment = async function () {
        let result = await MinunZTEAxon30Logic.getCurrentAdjustment();
        if (result) {
            for (let key of ["r", "g", "b", "a"]) {
                data[`slider-${key}`] = result[key];
            }
            data["slider-notch"] = result["notch"];
            if (!data["slider-notch"]) {
                data["slider-notch"] = 0;
            }
            for (let key in sliders) {
                sliders[key].reload();
            }
            let r = Math.round(data["slider-r"] * 255);
            let g = Math.round(data["slider-g"] * 255);
            let b = Math.round(data["slider-b"] * 255);
            let a = data["slider-a"];
            let notch = data["slider-notch"];
            document.querySelector("#sample-hole").style["background-color"] = `rgba(${r}, ${g}, ${b}, ${a})`;
            updateNotch(notch);
            window.MinunZTEAxon30Logic.updateLocalAdjustment(
                data["slider-r"], data["slider-g"], data["slider-b"], data["slider-a"],
                data["slider-notch"]);
        }
    };

    reloadCurrentAdjustment().catch((error) => {
        console.error(error);
    });

    const installSlider = function (id, label) {

        let value = 60 + data[id] * 240;

        let slider = document.querySelector(`#${id}`);

        sliders[id] = {
            "dom": slider,
            "reload": function () {
                value = 60 + data[id] * 240;
                slider.querySelector(".progress").style.width = `${value - 40}px`;
                document.querySelector(`#${id}-label`).textContent = `${label}: ${(data[id] * 100).toFixed(1)}%`;
            }
        };
        sliders[id].reload();

        const updateValue = function () {
            slider.querySelector(".progress").style.width = `${value - 40}px`;
            data[id] = (value - 60) / 240;
            let r = Math.round(data["slider-r"] * 255);
            let g = Math.round(data["slider-g"] * 255);
            let b = Math.round(data["slider-b"] * 255);
            let a = data["slider-a"];
            let notch = data["slider-notch"];
            document.querySelector("#sample-hole").style["background-color"] = `rgba(${r}, ${g}, ${b}, ${a})`;
            updateNotch(notch);
            window.MinunZTEAxon30Logic.updateLocalAdjustment(
                data["slider-r"], data["slider-g"], data["slider-b"], data["slider-a"],
                data["slider-notch"]);
            sliders[id].reload();
        };
        updateValue();

        let from = null;
        let fromPointerID = null;

        document.querySelector(`#${id}-caption .decrease`).addEventListener("click", function (event) {

            let from = (value - 60) / 240;

            from = (Math.round(from * 1000) - 1) / 1000;

            value = from * 240 + 60;
            if (value < 60) {
                value = 60;
            }
            updateValue();

        });

        document.querySelector(`#${id}-caption .increase`).addEventListener("click", function (event) {

            let from = (value - 60) / 240;

            from = (Math.round(from * 1000) + 1) / 1000;

            value = from * 240 + 60;
            if (value > 300) {
                value = 300;
            }
            updateValue();

        });

        slider.addEventListener("pointerdown", function (event) {
            if (fromPointerID !== null) {
                return;
            }
            fromPointerID = event.pointerId;
            from = value - event.screenX;
            slider.setPointerCapture(event.pointerId);
            event.preventDefault();
            event.stopPropagation();
        });

        slider.addEventListener("pointermove", function (event) {
            if (event.pointerId !== fromPointerID) {
                return;
            }
            value = from + event.screenX;
            if (value < 60) { value = 60; }
            if (value > 300) { value = 300; }
            updateValue();
            event.preventDefault();
            event.stopPropagation();
        });

        slider.addEventListener("pointerup", function (event) {
            if (event.pointerId !== fromPointerID) {
                return;
            }
            fromPointerID = null;
            from = null;
            slider.releasePointerCapture(event.pointerId);
            event.preventDefault();
            event.stopPropagation();
        });

        slider.addEventListener("pointercancel", function (event) {
            if (event.pointerId !== fromPointerID) {
                return;
            }
            fromPointerID = null;
            from = null;
            slider.releasePointerCapture(event.pointerId);
            event.preventDefault();
            event.stopPropagation();
        });

    };

    installSlider("slider-r", "红色");
    installSlider("slider-g", "绿色");
    installSlider("slider-b", "蓝色");
    installSlider("slider-a", "不透明度");
    installSlider("slider-notch", "挖孔和刘海模拟");

    for (let node of document.querySelectorAll(".color-well")) ((node) => {
        node.addEventListener("click", function (event) {
            document.querySelector("#test-background").style["background"] = `linear-gradient(to bottom, ${node.style["background-color"]} 0%, #fff0 100%)`;
            for (let node of document.querySelectorAll(".color-well")) {
                node.classList.remove("selected");
            }
            node.classList.add("selected");
        });
    })(node);

    const confirmSystemPermission = async function () {

        if (!(await MinunZTEAxon30Logic.isOverlayPermissionGranted())) {
            document.querySelector("#permission.mask").classList.add("visible");
        } else {
            document.querySelector("#permission.mask").classList.remove("visible");
            MinunZTEAxon30Logic.showAdjustmentOverlay().catch((error) => {
                // Do nothing
            });
            // MinunZTEAxon30Logic.autoenableAccessibilityService().catch((error) => {
            //     // Do nothing
            // });
        }

    };

    const updateDeviceStates = async function () {

        let states = await MinunZTEAxon30Logic.getCurrentDeviceStates();

        if (states.device) {
            document.querySelector("#current-device-id").textContent = states.device;
        } else {
            document.querySelector("#current-device-id").textContent = "未知";
        }

        if (states.temperature > -250) {
            document.querySelector("#current-temperature").textContent = states.temperature.toFixed(1) + "°C";
        } else {
            document.querySelector("#current-temperature").textContent = "未知";
        }

        if (states.brightness) {
            document.querySelector("#current-brightness").textContent = Math.round(states.brightness * 100)+ "%";
        } else {
            document.querySelector("#current-brightness").textContent = "未知";
        }

    };

    confirmSystemPermission().catch((error) => {
        // Do nothing
    });
    reloadCurrentAdjustment().catch((error) => {
        // Do nothing
    });
    updateDeviceStates().catch((error) => {
        // Do nothing
    });

    MinunZTEAxon30Logic.addPermissionChangeCallback(() => {
        confirmSystemPermission().catch((error) => {
            // Do nothing
        });
        reloadCurrentAdjustment().catch((error) => {
            // Do nothing
        });
    });

    setInterval(() => {
        updateDeviceStates().catch((error) => {
            // Do nothing
        });
    }, 3000);

});
