(function () {
	var cordovaRef = window.PhoneGap || window.cordova || window.Cordova;
	var queue = [];
	var runInterval = 1000;
	var running = false;
	var runner;

	function TagManager() { }

	// initialize google analytics with an account ID and the min number of seconds between posting
	//
	// id = the GTM account ID of the form 'GTM-000000'
	// period = the minimum interval for transmitting tracking events if any exist in the queue
	TagManager.prototype.init = function (success, fail, id, period) {
		runner = setInterval(run, runInterval);
		running = true;
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'initGTM',
			success: success,
			fail: fail,
			id: id,
			period: period
		});
	};

	// log an event
	//
	// category = The event category. This parameter is required to be non-empty.
	// eventAction = The event action. This parameter is required to be non-empty.
	// eventLabel = The event label. This parameter may be a blank string to indicate no label.
	// eventValue = The event value. This parameter may be -1 to indicate no value.
	TagManager.prototype.trackEvent = function (success, fail, category, eventAction, eventLabel, eventValue) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'trackEvent',
			success: success,
			fail: fail,
			category: category,
			eventAction: eventAction,
			eventLabel: eventLabel,
			eventValue: eventValue
		});
	};

	TagManager.prototype.pushImpressions = function (success, fail, item, list, currencyCode) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushImpressions',
			success: success,
			fail: fail,
			item : item,
			list : list,
			currencyCode : currencyCode
		});
	};

	TagManager.prototype.pushProductClick = function (success, fail, item, list) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushProductClick',
			success: success,
			fail: fail,
			item: item,
			list: list
		});
	};

	TagManager.prototype.pushDetailView = function (success, fail, item) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushDetailView',
			success: success,
			fail: fail,
			item : item
		});
	};

	TagManager.prototype.pushAddToCart = function (success, fail, item, currencyCode) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushAddToCart',
			success: success,
			fail: fail,
			item : item,
			currencyCode : currencyCode
		});
	};

	TagManager.prototype.pushRemoveFromCart = function (success, fail, item) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushRemoveFromCart',
			success: success,
			fail: fail,
			item : item
		});
	};

	TagManager.prototype.pushCheckout = function (success, fail, stepNo, products, option, screenName) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushCheckout',
			success: success,
			fail: fail,
			stepNo: stepNo,
			products : products,
			option : option,
			screenName : screenName
		});
	};

	TagManager.prototype.pushTransaction = function (success, fail, transaction, transactionItems) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushTransaction',
			success: success,
			fail: fail,
			transaction: transaction,
			transactionItems: transactionItems
		});
	};

	TagManager.prototype.pushImpressions = function (success, fail, items) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushImpressions',
			success: success,
			fail: fail,
			items: items
		});
	};

	TagManager.prototype.pushEvent = function (success, fail, eventData) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'pushEvent',
			success: success,
			fail: fail,
			eventData: eventData
		});
	};


	// log a page view
	//
	// pageURL = the URL of the page view
	TagManager.prototype.trackPage = function (success, fail, pageURL) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'trackPage',
			success: success,
			fail: fail,
			pageURL: pageURL
		});
	};

	// force a dispatch to Tag Manager
	TagManager.prototype.dispatch = function (success, fail) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'dispatch',
			success: success,
			fail: fail
		});
	};

	// exit the TagManager instance and stop setInterval
	TagManager.prototype.exit = function (success, fail) {
		var timestamp = new Date().getTime();
		queue.push({
			timestamp: timestamp,
			method: 'exitGTM',
			success: success,
			fail: fail
		});
	};

	if (cordovaRef && cordovaRef.addConstructor) {
		cordovaRef.addConstructor(init);
	} else {
		init();
	}

	function init() {
		if (!window.plugins) {
			window.plugins = {};
		}
		if (!window.plugins.TagManager) {
			window.plugins.TagManager = new TagManager();
		}
	}

	function run() {
		if (queue.length > 0) {
			var item = queue.shift();
			if (item.method === 'initGTM') {
				cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.id, item.period]);
			} else if (item.method === 'trackEvent') {
				cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.category, item.eventAction, item.eventLabel, item.eventValue]);
			} else if (item.method === 'pushEvent') {
				cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.eventData]);
            } else if (item.method === 'pushImpressions') {
               cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.item, item.list, item.currencyCode]);
            } else if (item.method === 'pushProductClick') {
               cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.item, item.list]);
            } else if (item.method === 'pushDetailView') {
               cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.item]);
            } else if (item.method === 'pushAddToCart') {
                cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.item, item.currencyCode]);
            }  else if (item.method === 'pushRemoveFromCart') {
                cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.item]);
			} else if (item.method === 'pushCheckout') {
			    cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.stepNo, item.products, item.option, item.screenName]);
			} else if (item.method === 'pushTransaction') {
                cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.transaction, item.transactionItems]);
            } else if (item.method === 'trackPage') {
				cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, [item.pageURL]);
			} else if (item.method === 'dispatch') {
				cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, []);
			} else if (item.method === 'exitGTM') {
				cordovaRef.exec(item.success, item.fail, 'TagManager', item.method, []);
				clearInterval(runner);
				running = false;
			}
		}
	}

	if (typeof module != 'undefined' && module.exports) {
		module.exports = new TagManager();
	}
})();
/* End of Temporary Scope. */