{

    const rawAPI = window[".MinunZTEAxon30API"];

    let apis = Object.create(null);
    let callbacks = Object.create(null);

    let nextCallID = 1;

    window.MinunZTEAxon30Callbacks = new Proxy(Object.create(null), {
        "get": function (target, key, receiver) {
            return function (error, result) {
                let callback = callbacks[key];
                if (!callback) {
                    console.error(`Callback[${key}] not found`); return;
                }
                delete callbacks[key];
                try {
                    if (error) {
                        callback[1](new Error(`APIError: ${error}`));
                    } else {
                        callback[0](result);
                    }
                } catch (error) {
                    console.error(error);
                }
            }
        }
    });

    window.MinunZTEAxon30API = new Proxy(Object.create(null), {
        "get": function (target, key, receiver) {
            if (!apis[key]) {
                apis[key] = function (... callArguments) {
                    return new Promise(function (resolve, reject) {
                        let callID = nextCallID; ++nextCallID;
                        callbacks[callID] = [resolve, reject];
                        rawAPI.callAPI(key, JSON.stringify({
                            "arguments": Array.prototype.slice.call(callArguments, 0),
                        }), `window.MinunZTEAxon30Callbacks[${callID}]`);
                    });
                };
            }
            return apis[key];
        }
    });

}