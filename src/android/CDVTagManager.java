/**
 * Copyright (c) 2014 Jared Dickson
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jareddickson.cordova.tagmanager;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.DataLayer;
import com.google.android.gms.tagmanager.TagManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.mchannel.fashionforall.R;

/**
 * This class echoes a string called from JavaScript.
 */
public class CDVTagManager extends CordovaPlugin {

    private static final long TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS = 2000;
    private static final String TAG = "CDVTagManager";
    private boolean initialized = false;


    public CDVTagManager() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) {
        if (action.equals("initGTM")) {
            try {
                // Set the dispatch interval
                // GAServiceManager.getInstance().setLocalDispatchPeriod(args.getInt(1));
                TagManager tagManager = TagManager.getInstance(this.cordova.getActivity().getApplicationContext());
                tagManager.setVerboseLoggingEnabled(true);

                String containerId = args.getString(0);

                // Passing '0' (Non Existing) as the resource ID so that fresh container will be loaded.
                PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(containerId, 0);
                pending.setResultCallback(new ResultCallback<ContainerHolder>() {
                    @Override
                    public void onResult(@NonNull ContainerHolder containerHolder) {
                        ContainerHolderSingleton.setContainerHolder(containerHolder);
                        Container container = containerHolder.getContainer();
                        if (!containerHolder.getStatus().isSuccess()) {
                            Log.e(TAG, "Failure loading container");
                            return;
                        }
                        ContainerLoadedCallback.registerCallbacksForContainer(container);
                        containerHolder.setContainerAvailableListener(new ContainerLoadedCallback());
                        initialized = true;

                    }
                }, TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS, TimeUnit.MILLISECONDS);

                callback.success("initGTM - id = " + args.getString(0) + "; interval = " + args.getInt(1) + " seconds");
                return true;
            } catch (final Exception e) {
                callback.error(e.getMessage());
            }
        } else if (action.equals("exitGTM")) {
            try {
                initialized = false;
                callback.success("exitGTM");
                return true;
            } catch (final Exception e) {
                callback.error(e.getMessage());
            }
        } else if (action.equals("dispatch")) {
            if (initialized) {
                try {
                    TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).dispatch();
                    callback.success("dispatch sent");
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("dispatch failed - not initialized");
            }
        } else if (action.equals("trackEvent")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    int value;
                    try {
                        value = args.getInt(3);
                    } catch (Exception e) {
                        value = 0;
                    }
                    dataLayer.push(DataLayer.mapOf("event", "interaction", "target", args.getString(0), "action", args.getString(1), "target-properties", args.getString(2), "value", value));
                    callback.success("trackEvent - category = " + args.getString(0) + "; action = " + args.getString(1) + "; label = " + args.getString(2) + "; value = " + value);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("trackEvent failed - not initialized");
            }
        } else if (action.equals("pushEvent")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.push(objectMap(args.getJSONObject(0)));
                    callback.success("pushEvent: " + dataLayer.toString());
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushEvent failed - not initialized");
            }
        } else if (action.equals("trackPage")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.pushEvent("content-view", DataLayer.mapOf("content-name", args.get(0)));
                    TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).dispatch();

                    callback.success("trackPage - url = " + args.getString(0));

                    dataLayer.push("event", null);
                    dataLayer.push("content-name", null);

                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("trackPage failed - not initialized");
            }
        } else if (action.equals("pushImpression")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    JSONObject item = args.getJSONObject(0);
                    String list = args.getString(1);
                    String currencyCode = args.getString(2);

                    Map<String, Object> itemMap = DataLayer.mapOf(
                            "name", item.getString("name"),
                            "id", item.getString("id"),
                            "price", item.getString("price"),
                            "list", list);

                    dataLayer.pushEvent("productImpression", DataLayer.mapOf(
                            "ecommerce", DataLayer.mapOf(
                                    "currencyCode", currencyCode,
                                    "impressions", DataLayer.listOf(itemMap)),
                            "content-name", item.get("name")
                    ));

                    callback.success("pushImpression: " + dataLayer.toString());
                    dataLayer.push("ecommerce", null);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushImpression failed - not initialized");
            }
        } else if (action.equals("pushProductClick")) {
            if (initialized) {
                try {
                    JSONObject product = args.getJSONObject(0);
                    Map<String, Object> itemMap = getProductMap(product);
                    String list = args.getString(1);

                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();

                    int value;
                    try {
                        value = (int) Float.parseFloat(product.getString("price"));
                    } catch (Exception e) {
                        value = 0;
                    }

                    dataLayer.pushEvent("productClick", DataLayer.mapOf(
                            "value", value,
                            "ecommerce", DataLayer.mapOf(
                                    "click", DataLayer.mapOf(
                                            "actionField", DataLayer.mapOf(
                                                    "list", list),
                                            "products", DataLayer.listOf(
                                                    itemMap)))));
                    callback.success("pushProductClick = " + product);
                    dataLayer.push("value", null);
                    dataLayer.push("ecommerce", null);
                    return true;

                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushProductClick failed - not initialized");
            }
        } else if (action.equals("pushDetailView")) {
            if (initialized) {
                try {
                    JSONObject product = args.getJSONObject(0);
                    Map<String, Object> itemMap = getProductMap(product);
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.pushEvent("detailView",
                            DataLayer.mapOf(
                                    "ecommerce", DataLayer.mapOf(
                                            "detail", DataLayer.mapOf(
                                                    "products", DataLayer.listOf(itemMap))),
                                    "content-name", product.get("name")));

                    callback.success("pushDetailView = " + product);
                    dataLayer.push("ecommerce", null);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushDetailView failed - not initialized");
            }
        } else if (action.equals("pushAddToCart")) {
            if (initialized) {
                try {
                    JSONObject product = args.getJSONObject(0);
                    Map<String, Object> itemMap = getProductMap(product);

                    String currencyCode = args.getString(1);
                    int value;
                    try {
                        value = (int) Float.parseFloat(product.getString("price"));
                    } catch (Exception e) {
                        value = 0;
                    }
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.pushEvent("addToCart",
                            DataLayer.mapOf(
                                    "ecommerce", DataLayer.mapOf(
                                            "currencyCode", currencyCode,
                                            "add", DataLayer.mapOf(
                                                    "products", DataLayer.listOf(itemMap))),
                                    "value", value));
                    callback.success("pushAddToCart = " + args.getString(0) + " currencyCode = " + currencyCode);
                    dataLayer.push("ecommerce", null);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushAddToCart failed - not initialized");
            }
        } else if (action.equals("pushRemoveFromCart")) {
            if (initialized) {
                try {
                    JSONObject product = args.getJSONObject(0);
                    Map<String, Object> itemMap = getProductMap(product);

                    int value;
                    try {
                        value = (int) Float.parseFloat(product.getString("price"));
                    } catch (Exception e) {
                        value = 0;
                    }
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.pushEvent("removeFromCart",
                            DataLayer.mapOf(
                                    "ecommerce", DataLayer.mapOf(
                                            "remove", DataLayer.mapOf(
                                                    "products", DataLayer.listOf(itemMap))),
                                    "value", value));
                    callback.success("pushRemoveCart = " + args.getString(0));
                    dataLayer.push("ecommerce", null);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushRemoveFromCart failed - not initialized");
            }
        } else if (action.equals("pushCheckout")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();

                    int stepNo = args.getInt(0);
                    JSONArray productsJSONArray = args.getJSONArray(1);
                    String option = args.getString(2);
                    String screenName = args.getString(3);

                    ArrayList items = new ArrayList<Map<String, Object>>();

                    for (int i = 0; i < productsJSONArray.length(); i++) {
                        JSONObject item = productsJSONArray.getJSONObject(i);

                        Map<String, Object> itemMap = getProductMap(item);
                        items.add(itemMap);
                    }

                    List<Object> products = DataLayer.listOf(items.toArray(new Object[items.size()]));

                    Map<String, Object> actionField;

                    if (option.isEmpty()) {
                        actionField = DataLayer.mapOf("step", stepNo);
                    } else {
                        actionField = DataLayer.mapOf("step", stepNo, "option", option);
                    }

                    dataLayer.pushEvent("checkout",
                            DataLayer.mapOf("content-name", screenName,
                                    "ecommerce", DataLayer.mapOf(
                                            "checkout", DataLayer.mapOf(
                                                    "actionField", actionField,
                                                    "products", products))));
                    callback.success("pushCheckout: " + dataLayer.toString());
                    dataLayer.push("ecommerce", null);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("dispatch failed - not initialized");
            }
        } else if (action.equals("pushTransaction")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    JSONObject transaction = args.getJSONObject(0);
                    JSONArray transactionItems = args.getJSONArray(1);
                    ArrayList items = new ArrayList<Map<String, Object>>();

                    for (int i = 0; i < transactionItems.length(); i++) {
                        JSONObject item = transactionItems.getJSONObject(i);

                        Map<String, Object> itemMap = getProductMap(item);
                        items.add(itemMap);
                    }

                    List<Object> products = DataLayer.listOf(items.toArray(new Object[items.size()]));

                    String contentName = "Payment Response";

                    dataLayer.pushEvent("orderPlaced",
                            DataLayer.mapOf("content-name", contentName,
                                    "ecommerce", DataLayer.mapOf("purchase", DataLayer.mapOf(
                                            "actionField", DataLayer.mapOf(
                                                    "id", transaction.getString("transactionId"),
                                                    "affiliation", transaction.getString("transactionAffiliation"),
                                                    "revenue", transaction.getString("transactionTotal"),
                                                    "tax", transaction.getString("transactionTax"),
                                                    "shipping", transaction.getString("transactionShipping")),
                                            "products", products
                                    ))));

                    callback.success("pushTransaction: " + dataLayer.toString());
                    dataLayer.push("ecommerce", null);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushTransaction failed - not initialized");
            }
        }

        return false;
    }

    private Map<String, Object> getProductMap(JSONObject item) throws JSONException {
        return DataLayer.mapOf(
                "name", item.getString("name"),
                "id", item.getString("id"),
                "price", item.getString("price"),
                "quantity", item.has("quantity") ? item.getString("quantity") : "1");
    }

    private Map<String, Object> objectMap(JSONObject o) throws JSONException {
        if (o.length() == 0) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<String, Object>(o.length());
        Iterator it = o.keys();
        String key;
        Object value;
        while (it.hasNext()) {
            key = (String) it.next();
            value = o.has(key) ? o.get(key) : null;
            map.put(key, value);
        }
        return map;
    }

    private static class ContainerLoadedCallback implements ContainerHolder.ContainerAvailableListener {
        static void registerCallbacksForContainer(Container container) {
        }

        @Override
        public void onContainerAvailable(ContainerHolder containerHolder, String containerVersion) {
            // We load each container when it becomes available.
            Container container = containerHolder.getContainer();
            registerCallbacksForContainer(container);
        }
    }

    private static class ContainerHolderSingleton {
        private static ContainerHolder containerHolder;

        /**
         * Utility class; don't instantiate.
         */
        private ContainerHolderSingleton() {
        }

        static ContainerHolder getContainerHolder() {
            return containerHolder;
        }

        static void setContainerHolder(ContainerHolder c) {
            containerHolder = c;
        }
    }
}
