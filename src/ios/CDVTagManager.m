/**
 * Copyright (c) 2014 Jared Dickson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#import "CDVTagManager.h"

@implementation CDVTagManager
- (void) initGTM:(CDVInvokedUrlCommand*)command
{
    NSString    *callbackId = command.callbackId;
    NSString    *accountID = [command.arguments objectAtIndex:0];
    NSInteger   dispatchPeriod = [[command.arguments objectAtIndex:1] intValue];
    
    inited = FALSE;
    self.tagManager = [TAGManager instance];
    
    // Modify the log level of the logger to print out not only
    // warning and error messages, but also verbose, debug, info messages.
    [self.tagManager.logger setLogLevel:kTAGLoggerLogLevelVerbose];
    
    // Set the dispatch interval
    self.tagManager.dispatchInterval = dispatchPeriod;
    
    // Open a container.
    [TAGContainerOpener openContainerWithId:accountID
                                 tagManager:self.tagManager
                                   openType:kTAGOpenTypePreferNonDefault
                                    timeout:nil
                                   notifier:self];
    [self successWithMessage:[NSString stringWithFormat:@"initGTM: accountID = %@; Interval = %ld seconds",accountID, (long)dispatchPeriod] toID:callbackId];
}

- (void) containerAvailable:(TAGContainer *)container {
    // Important note: containerAvailable may be called from a different thread, marshall the
    // notification back to the main thread to avoid a race condition with viewDidAppear.
    inited = TRUE;
    dispatch_async(dispatch_get_main_queue(), ^{
        self.container = container;
    });
}

-(void) exitGTM:(CDVInvokedUrlCommand*)command
{
    NSString *callbackId = command.callbackId;
    
    if (inited)
        [self.container close];
    
    [self successWithMessage:@"exitGTM" toID:callbackId];
}

- (void) trackEvent:(CDVInvokedUrlCommand*)command
{
    NSString        *callbackId = command.callbackId;
    NSString        *category = [command.arguments objectAtIndex:0];
    NSString        *eventAction = [command.arguments objectAtIndex:1];
    NSString        *eventLabel = [command.arguments objectAtIndex:2];
    NSNumber *eventValue = @0;
    id valueObject = [command.arguments objectAtIndex:3];
    if (![valueObject isEqual:[NSNull null]])
    {
        eventValue = [NSNumber numberWithInt:[valueObject intValue]];
    }
    
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        [dataLayer push:@{@"event":@"interaction", @"target":category, @"action":eventAction, @"target-properties":eventLabel, @"value":eventValue}];
        [self successWithMessage:@"trackEvent" toID:callbackId];
    }
    else
        [self failWithMessage:@"trackEvent failed - not initialized" toID:callbackId withError:nil];
}

- (void) pushEvent:(CDVInvokedUrlCommand*)command
{
    NSString        *callbackId = command.callbackId;
    NSDictionary    *eventData = [command.arguments objectAtIndex:0];
    
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        [dataLayer push:eventData];
        [self successWithMessage:@"pushEvent" toID:callbackId];
    }
    else
        [self failWithMessage:@"trackEvent failed - not initialized" toID:callbackId withError:nil];
}

- (void) trackPage:(CDVInvokedUrlCommand*)command
{
    NSString            *callbackId = command.callbackId;
    NSString            *pageURL = [command.arguments objectAtIndex:0];
    
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        [dataLayer push:@{@"event": @"content-view", @"content-name":pageURL}];
    }
    else
        [self failWithMessage:@"trackPage failed - not initialized" toID:callbackId withError:nil];
}

- (void) pushImpression : (CDVInvokedUrlCommand *) command
{
    NSString *callbackId = command.callbackId;
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSDictionary *product = [command.arguments objectAtIndex:0];
        NSString *list = [command.arguments objectAtIndex:1];
        [product setValue:list forKey:@"list"];
        NSString *currencyCode = [command.arguments objectAtIndex:2];
        
        NSArray *productsArray = @[product];
        
        NSDictionary *data = @{@"event":@"productImpression",
                               @"content-name" : [product objectForKey:@"name"],
                               @"ecommerce" : @{
                                       @"currencyCode" : currencyCode,
                                       @"impressions": productsArray
                                       }
                               };
        
        [dataLayer push:data];
        [dataLayer push:@{@"ecommerce" : [NSNull null]}];
    }
    else
        [self failWithMessage:@"pushImpression failed - not initialized" toID:callbackId withError:nil];
}

- (void) pushProductClick : (CDVInvokedUrlCommand *) command
{
    NSString            *callbackId = command.callbackId;
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSDictionary *product = [command.arguments objectAtIndex:0];
        NSString *list = [command.arguments objectAtIndex:1];
        
        [self speficyQuantity:product];
        
        NSNumber *value = [NSNumber numberWithInt: [[product objectForKey:@"price"] intValue]];
        NSArray *productsArray = @[product];
        
        NSDictionary *data = @{@"event":@"productClick",
                               @"value" : value,
                               @"ecommerce" : @{
                                       @"click": @{
                                               @"actionField" : @{@"list" : list},
                                               @"products": productsArray
                                               }
                                       }
                               };
        
        [dataLayer push: data];
        [dataLayer push: @{@"ecommerce": [NSNull null]}];
    }else
        [self failWithMessage:@"pushProductClick failed - not initialized" toID:callbackId withError:nil];
    
}

- (void) pushDetailView : (CDVInvokedUrlCommand *) command
{
    NSString *callbackId = command.callbackId;
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSDictionary *product = [command.arguments objectAtIndex:0];
        [self speficyQuantity:product];
        NSArray *productsArray = @[product];
        
        NSDictionary *data = @{@"event":@"detailView",
                               @"content-name" : [product objectForKey:@"name"],
                               @"ecommerce" : @{
                                       @"detail": @{
                                               @"products": productsArray
                                               }
                                       }
                               };
        
        [dataLayer push:data];
        [dataLayer push:@{@"ecommerce" : [NSNull null]}];
    }
    else
        [self failWithMessage:@"pushDetailView failed - not initialized" toID:callbackId withError:nil];
}

- (void) pushRemoveFromCart : (CDVInvokedUrlCommand *) command
{
    NSString            *callbackId = command.callbackId;
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSDictionary *product = [command.arguments objectAtIndex:0];
        
        [self speficyQuantity:product];
        
        NSNumber *value = [NSNumber numberWithInt: [[product objectForKey:@"price"] intValue]];
        NSArray *productsArray = @[product];
        
        NSDictionary *data = @{@"event":@"removeFromCart",
                               @"value" : value,
                               @"ecommerce" : @{
                                       @"remove": @{
                                               @"products": productsArray
                                               }
                                       }
                               };
        
        [dataLayer push: data];
        [dataLayer push: @{@"ecommerce": [NSNull null]}];
        
    }else
        [self failWithMessage:@"pushRemoveFromCart failed - not initialized" toID:callbackId withError:nil];
    
}

- (void) pushAddToCart : (CDVInvokedUrlCommand *) command
{
    NSString            *callbackId = command.callbackId;
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSDictionary *product = [command.arguments objectAtIndex:0];
        
        [self speficyQuantity:product];
        
        NSString *currencyCode = [command.arguments objectAtIndex:1];
        NSNumber *value = [NSNumber numberWithInt: [[product objectForKey:@"price"] intValue]];
        NSArray *productsArray = @[product];
        
        NSDictionary *data = @{@"event":@"addToCart",
                               @"value" : value,
                               @"ecommerce" : @{
                                       @"currencyCode": currencyCode,
                                       @"add": @{
                                               @"products": productsArray
                                               }
                                       }
                               };
        
        [dataLayer push: data];
        [dataLayer push: @{@"ecommerce": [NSNull null]}];
        
    }else
        [self failWithMessage:@"pushAddToCart failed - not initialized" toID:callbackId withError:nil];
    
}

- (void) pushCheckout : (CDVInvokedUrlCommand *) command
{
    NSString            *callbackId = command.callbackId;
    if (inited)
    {
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSNumber *stepNo = [NSNumber numberWithInt: [[command.arguments objectAtIndex:0] intValue]];
        NSArray *productsJSONArray = [command.arguments objectAtIndex:1];
        
        for (NSDictionary *item in productsJSONArray){
            [self speficyQuantity:item];
        }
        
        NSString *option = [command.arguments objectAtIndex:2];
        NSString *screenName = [command.arguments objectAtIndex:3];
        
        NSDictionary *data = @{@"event":@"checkout",
                               @"content-name":screenName,
                               @"ecommerce" : @{
                                       @"checkout": @{
                                               @"actionField" : @{
                                                       @"step" : stepNo,
                                                       @"option": option
                                                       },
                                               @"products": productsJSONArray
                                               }
                                       }
                               };
        
        [dataLayer push: data];
        
        [dataLayer push: @{@"ecommerce": [NSNull null]}];
        
    } else
        [self failWithMessage:@"pushCheckout failed - not initialized" toID:callbackId withError:nil];
}

- (void) pushTransaction : (CDVInvokedUrlCommand *) command
{
    NSString            *callbackId = command.callbackId;
    if (inited)
    {
        NSDictionary *transaction = [command.arguments objectAtIndex:0];
        NSArray *transactionItems = [command.arguments objectAtIndex:1];
        for (NSDictionary *item in transactionItems){
            [self speficyQuantity:item];
        }
        
        NSString *contentName = @"Payment Response";
        
        TAGDataLayer *dataLayer = [TAGManager instance].dataLayer;
        NSDictionary *data = @{@"event":@"orderPlaced",
                               @"content-name":contentName,
                               @"ecommerce" : @{
                                       @"purchase": @{
                                               @"actionField" : @{
                                                       @"id" : [transaction objectForKey:@"transactionId"],
                                                       @"affiliation": [transaction objectForKey:@"transactionAffiliation"],
                                                       @"revenue": [transaction objectForKey:@"transactionTotal"],
                                                       @"tax": [transaction objectForKey:@"transactionTax"],
                                                       @"shipping": [transaction objectForKey:@"transactionShipping"]
                                                       },
                                               @"products": transactionItems
                                               }
                                       }
                               };
        [dataLayer push: data];
        
        // Clear the Data Layer
        [dataLayer push: @{@"ecommerce": [NSNull null]}];
        
    }else
        [self failWithMessage:@"pushTransaction failed - not initialized" toID:callbackId withError:nil];
}

- (void) speficyQuantity : (NSDictionary *) product
{
    if ([product objectForKey:@"quantity"]  == nil){
        [product setValue:@"1" forKey:@"quantity"];
    }
}

- (void) dispatch:(CDVInvokedUrlCommand*)command
{
    NSString            *callbackId = command.callbackId;
    
    if (inited)
    {
        [self.tagManager dispatch];
    }
    else
        [self failWithMessage:@"dispatch failed - not initialized" toID:callbackId withError:nil];
}

-(void) successWithMessage:(NSString *)message toID:(NSString *)callbackID
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

-(void) failWithMessage:(NSString *)message toID:(NSString *)callbackID withError:(NSError *)error
{
    NSString        *errorMessage = (error) ? [NSString stringWithFormat:@"%@ - %@", message, [error localizedDescription]] : message;
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
    
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

-(void)dealloc
{
    [self.container close];
    // [super dealloc];
}

@end
