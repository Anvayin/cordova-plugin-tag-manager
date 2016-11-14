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

                // TODO - Provide a way to specify the default container through plugin configuration
                PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(containerId, R.raw.defaultcontainer_binary);
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
        } else if (action.equals("pushImpressions")) {
            // TODO - Reimplement this with correct values.
            if (initialized) {
                try {
            /*

                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    // Product impressions are sent by pushing an impressions object
                    // containing one or more impressionFieldObjects.
                    // Sample Data - Replace with real data.
                    dataLayer.push("ecommerce",
                            DataLayer.mapOf(
                                    "currencyCode", "EUR",                                  // Local currency is optional.
                                    "impressions", DataLayer.listOf(
                                            DataLayer.mapOf(
                                                    "name", "Triblend Android T-Shirt",             // Name or ID is required.
                                                    "id", "12345",
                                                    "price", "15.25",
                                                    "brand", "Google",
                                                    "category", "Google Apparel",
                                                    "variant", "Gray",
                                                    "list", "Search Results",
                                                    "position", 1),
                                            DataLayer.mapOf(
                                                    "name", "Donut Friday Scented T-Shirt",
                                                    "id", "67890",
                                                    "price", "33.75",
                                                    "brand", "Google",
                                                    "category", "Google Apparel",
                                                    "variant", "Black",
                                                    "list", "Search Results",
                                                    "position", 2))));
                    callback.success("pushImpressions: " + dataLayer.toString());

            */
                    callback.success("pushImpressions called. ");

                    return true;

                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushImpressions failed - not initialized");
            }
        } else if (action.equals("pushProductClick")) {
            if (initialized) {
                try {

                    JSONObject product = args.getJSONObject(0);
                    String list = args.getString(1);

                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();

                    String category = list;
                    String productAction = "Click";
                    String label = product.getString("name");

                    int value;
                    try {
                        value = (int) Float.parseFloat(product.getString("price"));
                    } catch (Exception e) {
                        value = 0;
                    }

                    dataLayer.push(DataLayer.mapOf(
                            "ecommerce", DataLayer.mapOf(
                                    "click", DataLayer.mapOf(
                                            "actionField", DataLayer.mapOf(
                                                    "list", list),                    // Optional list property.
                                            "products", DataLayer.listOf(
                                                    DataLayer.mapOf(
                                                            "name", product.get("name"),       // Name or ID is required.
                                                            "id", product.getString("id"),
                                                            "price", product.getString("price")
                                                    ))))));

                    dataLayer.push(DataLayer.mapOf("event", "interaction", "target", category, "action", productAction, "target-properties", label, "value", value));
                    Log.d(TAG, "Pushed click : " + dataLayer.toString());
                    callback.success("trackEvent - action = " + productAction + "; category = " + category + "; label = " + label + "; value = " + value);

                    dataLayer.push("event", null);
                    dataLayer.push("target", null);
                    dataLayer.push("action", null);
                    dataLayer.push("target-properties", null);
                    dataLayer.push("value", null);

                    dataLayer.push("ecommerce", null);
                    Log.d(TAG, "After Clearing : " + dataLayer.toString());

                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("trackEvent failed - not initialized");
            }

        } else if (action.equals("pushAddToCart")) {
            if (initialized) {
                try {
                    JSONObject product = args.getJSONObject(0);
                    String currencyCode = args.getString(1);
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    /*

                    Log.d(TAG, "Before pushAddToCart : " + dataLayer.toString());


                    // Measure adding a product to a shopping cart by using an "add"
                    // actionFieldObject and a list of productFieldObjects.
                    dataLayer.pushEvent("addToCart",
                            DataLayer.mapOf(
                                    "ecommerce", DataLayer.mapOf(
                                            "currencyCode", currencyCode,
                                            "add", DataLayer.mapOf(                             // 'add' actionFieldObject measures.
                                                    "products", DataLayer.listOf(
                                                            DataLayer.mapOf(
                                                                    "name", product.get("name"),
                                                                    "id", product.getString("id"),
                                                                    "price", product.getString("price"),
                                                                    "quantity", 1))))));

                    Log.d(TAG, "After pushAddToCart : " + dataLayer.toString());
                    */

                    callback.success("pushAddToCart = " + args.getString(0) + " currencyCode = " + currencyCode);
                    // Clear the Data Layer.
                    // dataLayer.push(DataLayer.mapOf("ecommerce", null));

                    /*
                    dataLayer.push(
                            DataLayer.mapOf(
                                    "ecommerce", DataLayer.mapOf(
                                            "currencyCode", null,
                                            "add", null)));
                                            */

                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushAddToCart failed - not initialized");
            }
        } else if (action.equals("trackPage")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.pushEvent("content-view", DataLayer.mapOf("content-name", args.get(0)));
                    TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).dispatch();

                    callback.success("trackPage - url = " + args.getString(0));

                    // Clear Data Layer.
                    dataLayer.push("event", null);
                    dataLayer.push("content-name", null);

                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("trackPage failed - not initialized");
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
        } else if (action.equals("pushTransaction")) {
            if (initialized) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    JSONObject transaction = args.getJSONObject(0);
                    JSONArray transactionItems = args.getJSONArray(1);
                    ArrayList items = new ArrayList<Map<String, Object>>();

                    for (int i = 0; i < transactionItems.length(); i++) {
                        JSONObject item = transactionItems.getJSONObject(i);

                        Map<String, Object> itemMap = DataLayer.mapOf(
                                "name", item.getString("name"),
                                "id", item.getString("id"),
                                "price", item.getString("price"),
                                "quantity", item.getString("quantity"));
                        items.add(itemMap);
                    }

                    List<Object> products = DataLayer.listOf(items.toArray(new Object[items.size()]));
                    // dataLayer.push();

                    String contentName = "Payment Response";

                    dataLayer.pushEvent("orderPlaced", DataLayer.mapOf("content-name", contentName, "ecommerce",
                            DataLayer.mapOf(
                                    "purchase", DataLayer.mapOf(
                                            "actionField", DataLayer.mapOf(
                                                    "id", transaction.getString("transactionId"),
                                                    "affiliation", transaction.getString("transactionAffiliation"),
                                                    "revenue", transaction.getString("transactionTotal"),
                                                    "tax", transaction.getString("transactionTax"),
                                                    "shipping", transaction.getString("transactionShipping")),
                                            "products", products
                                    ))));

                    callback.success("pushTransaction: " + dataLayer.toString());

                    Log.d(TAG, "Pushed Transaction Screen View : " + dataLayer.toString());
                    dataLayer.push("ecommerce", null);
                    Log.d(TAG, "After Clearing : " + dataLayer.toString());

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

    private void clearDataLayer() {
        DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
        // Clear the Data Layer as it persists the data
        dataLayer.push(DataLayer.mapOf("transactionId", null,
                "transactionTotal", null,
                "transactionAffiliation", null,
                "transactionTax", null,
                "transactionShipping", null,
                "transactionCurrency", null,
                "transactionProducts", null));

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
