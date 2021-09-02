window.addEventListener("load", async function () {

    document.body.addEventListener("contextmenu", function (event) {
        event.preventDefault();
        event.stopPropagation();
    });

    let colors = {
        "slider-r": 0,
        "slider-g": 0,
        "slider-b": 0,
        "slider-a": 0,
    };
    let sliders = Object.create(null);

    const reloadCurrentAdjustment = async function () {
        let result = await MinunZTEAxon30Logic.getCurrentAdjustment();
        if (result) {
            for (let key of ["r", "g", "b", "a"]) {
                colors[`slider-${key}`] = result[key];
            }
            for (let key in sliders) {
                sliders[key].reload();
            }
            let r = Math.round(colors["slider-r"] * 255);
            let g = Math.round(colors["slider-g"] * 255);
            let b = Math.round(colors["slider-b"] * 255);
            let a = colors["slider-a"];
            document.querySelector("#sample-hole").style["background-color"] = `rgba(${r}, ${g}, ${b}, ${a})`;
            window.MinunZTEAxon30Logic.updateLocalAdjustment(
                colors["slider-r"], colors["slider-g"], colors["slider-b"], colors["slider-a"]);
        }
    };

    reloadCurrentAdjustment().catch((error) => {
        console.error(error);
    });

    const installSlider = function (id, label) {

        let value = 60 + colors[id] * 240;

        let slider = document.querySelector(`#${id}`);

        sliders[id] = {
            "dom": slider,
            "reload": function () {
                value = 60 + colors[id] * 240;
                slider.querySelector(".progress").style.width = `${value - 40}px`;
                document.querySelector(`#${id}-label`).textContent = `${label}: ${(colors[id] * 100).toFixed(1)}%`;
            }
        };
        sliders[id].reload();

        const updateValue = function () {
            slider.querySelector(".progress").style.width = `${value - 40}px`;
            colors[id] = (value - 60) / 240;
            let r = Math.round(colors["slider-r"] * 255);
            let g = Math.round(colors["slider-g"] * 255);
            let b = Math.round(colors["slider-b"] * 255);
            let a = colors["slider-a"];
            document.querySelector("#sample-hole").style["background-color"] = `rgba(${r}, ${g}, ${b}, ${a})`;
            window.MinunZTEAxon30Logic.updateLocalAdjustment(
                colors["slider-r"], colors["slider-g"], colors["slider-b"], colors["slider-a"]);
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
            console.log("cance")
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

    confirmSystemPermission().catch((error) => {
        // Do nothing
    });
    reloadCurrentAdjustment().catch((error) => {
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

});
