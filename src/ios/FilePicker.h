/*
 * FilePicker.h
 *
 * Created by @jcesarmobile
 * Edited by @elizabethrego
 */

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface FilePicker : CDVPlugin <UIDocumentMenuDelegate,UIDocumentPickerDelegate>

@property (strong, nonatomic) CDVPluginResult * pluginResult;
@property (strong, nonatomic) CDVInvokedUrlCommand * command;
@property (nonatomic) BOOL returnWithDetail;

- (void)deviceSupported:(CDVInvokedUrlCommand*)command;
- (void)pickFile:(CDVInvokedUrlCommand*)command;

@end
