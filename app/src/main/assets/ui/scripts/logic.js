(async () => {

    const logic = Object.create(null);

    let colors = [0, 0, 0, 0];

    let permissinCallbacks = [];

    logic.callbackPermissionChanges = async function () {
        for (let callback of permissinCallbacks) {
            try {
                callback();
            } catch (error) {
                console.error(error);
            }
        }
    };

    logic.addPermissionChangeCallback = function (callback) {
        permissinCallbacks.push(callback);
    };

    logic.removePermissionChangeCallback = function (callback) {
        let index = permissinCallbacks.indexOf(callback);
        if (index !== -1) {
            permissinCallbacks.splice(index, 1);
        }
    };

    logic.clearAdjustmentOverlay = async function () {

        await window.MinunZTEAxon30API.clearAdjustmentOverlay();

    };

    logic.restoreAdjustmentOverlay = async function () {

        await window.MinunZTEAxon30API.restoreAdjustmentOverlay();

    };

    logic.showAdjustmentOverlay = async function () {

        if (!await window.MinunZTEAxon30API.isOverlayPermissionGranted()) {
            // TODO: show ui for permission warning;
            await window.MinunZTEAxon30API.navigateToPermissionManager();
            return;
        }

        return await window.MinunZTEAxon30API.showAdjustmentOverlay();

    };

    logic.hideAdjustmentOverlay = async function () {

        return await window.MinunZTEAxon30API.hideAdjustmentOverlay();

    };

    logic.updateLocalAdjustment = async function (r, g, b, a) {

        colors = [r, g, b, a];

    };

    logic.saveAdjustment = async function () {

        try {
            await window.MinunZTEAxon30API.setAdjustment(colors[0], colors[1], colors[2], colors[3], false);
        } catch (error) {
            console.error(error);
        }

    };

    logic.getCurrentAdjustment = async function () {

        return await window.MinunZTEAxon30API.getCurrentAdjustment();

    };

    logic.navigateToPermissionManager = async function () {

        return await window.MinunZTEAxon30API.navigateToPermissionManager();

    };

    logic.isOverlayPermissionGranted = async function () {

        return await window.MinunZTEAxon30API.isOverlayPermissionGranted();

    };

    window.MinunZTEAxon30Logic = logic;

    await window.MinunZTEAxon30API.setPermissionCheckCallbackScript("window.MinunZTEAxon30Logic.callbackPermissionChanges()");
    
})().catch((error) => {
    console.error(error);
})