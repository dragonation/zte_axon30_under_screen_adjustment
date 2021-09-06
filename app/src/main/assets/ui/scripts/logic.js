(async () => {

    const logic = Object.create(null);

    let colors = [0, 0, 0, 0, 0];

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

    logic.updateLocalAdjustment = async function (r, g, b, a, notch) {

        colors = [r, g, b, a, notch];

    };

    logic.saveAdjustment = async function (force) {

        if (colors[4] && (!force)) {
            document.querySelector("#warning").classList.add("visible");
            return;
        }

        try {
            await window.MinunZTEAxon30API.setAdjustment(colors[0], colors[1], colors[2], colors[3], colors[4], false);
        } catch (error) {
            console.error(error);
        }

        let data = (await MinunZTEAxon30Logic.listRecentAdjustments(100)).records;

        let r = MinunZTEAxon30Analysis.createDataMesh(data.map(record => [record.temperature, record.brightness, record.r]));
        let g = MinunZTEAxon30Analysis.createDataMesh(data.map(record => [record.temperature, record.brightness, record.g]));
        let b = MinunZTEAxon30Analysis.createDataMesh(data.map(record => [record.temperature, record.brightness, record.b]));
        let a = MinunZTEAxon30Analysis.createDataMesh(data.map(record => [record.temperature, record.brightness, record.a]));

        let analysis = {
            "brightness": a.brightness,
            "temperatures": a.temperatures,
            "r": r.points,
            "g": g.points,
            "b": b.points,
            "a": a.points,
        };

        await window.MinunZTEAxon30API.saveAnalysis(analysis);

    };

    logic.getCurrentAdjustment = async function () {

        return await window.MinunZTEAxon30API.getCurrentAdjustment();

    };

    logic.getCurrentDeviceStates = async function () {

        return await window.MinunZTEAxon30API.getCurrentDeviceStates();

    };

    logic.navigateToPermissionManager = async function () {

        return await window.MinunZTEAxon30API.navigateToPermissionManager();

    };

    logic.isOverlayPermissionGranted = async function () {

        return await window.MinunZTEAxon30API.isOverlayPermissionGranted();

    };

    logic.navigateToSettingPermission = async function () {

        return await window.MinunZTEAxon30API.navigateToSettingPermission();

    };

    logic.isWritingSystemPermissionGranted = async function () {

        return await window.MinunZTEAxon30API.isWritingSystemPermissionGranted();

    };

    logic.autoenableAccessibilityService = async function () {

        return await window.MinunZTEAxon30API.autoenableAccessibilityService();

    };

    logic.listRecentAdjustments = async function (limit) {

        return await window.MinunZTEAxon30API.listRecentAdjustments(limit);

    };

    logic.isAutofitEnabled = async function () {

        return await window.MinunZTEAxon30API.isAutofitEnabled();

    };

    logic.setAutofitEnabled = async function (enabled) {

        return await window.MinunZTEAxon30API.setAutofitEnabled(enabled);

    };

    logic.clearRecentAdjustments = async function () {

        return await window.MinunZTEAxon30API.clearRecentAdjustments();

    };

    logic.toggleAutofitEnabled = async function () {

        if (await logic.isAutofitEnabled()) {
            await window.MinunZTEAxon30API.setAutofitEnabled(false);
            document.querySelector("#trigger-autofit").textContent = "开启";
        } else {
            await window.MinunZTEAxon30API.setAutofitEnabled(true);
            document.querySelector("#trigger-autofit").textContent = "禁用";
        }

    };

    window.MinunZTEAxon30Logic = logic;

    await window.MinunZTEAxon30API.setPermissionCheckCallbackScript("window.MinunZTEAxon30Logic.callbackPermissionChanges()");
    
})().catch((error) => {
    console.error(error);
})