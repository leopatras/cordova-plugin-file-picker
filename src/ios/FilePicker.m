/*
 * FilePicker.m
 *
 * Created by @jcesarmobile
 * Edited by @elizabethrego
 */

#import "FilePicker.h"

@implementation FilePicker

- (void)deviceSupported:(CDVInvokedUrlCommand*)command {
    BOOL supported = NO;

    if (NSClassFromString(@"UIDocumentPickerViewController")) {
        supported = YES;
    }
    
    [self.commandDelegate sendPluginResult:[CDVPluginResult
                                            resultWithStatus:CDVCommandStatus_OK
                                            messageAsBool:supported]
                                callbackId:command.callbackId];
}

- (void)pickFile:(CDVInvokedUrlCommand*)command {
    
    self.command = command;
    id UTIs = [command.arguments objectAtIndex:0];
    self.returnWithDetail = [[command.arguments objectAtIndex:1] boolValue];
    BOOL supported = YES;
    
    NSArray* UTIsArray = nil;
    if ([UTIs isEqual:[NSNull null]]) {
        UTIsArray =  @[@"public.data"];
    } else if ([UTIs isKindOfClass:[NSString class]]){
        UTIsArray = @[UTIs];
    } else if ([UTIs isKindOfClass:[NSArray class]]){
        UTIsArray = UTIs;
    } else {
        supported = NO;
    }
    
    if (!NSClassFromString(@"UIDocumentPickerViewController")) {
        // Cannot show picker
        supported = NO;
    }
    
    if (supported) {
        self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
        [self.pluginResult setKeepCallbackAsBool:YES];
        [self displayDocumentPicker:UTIsArray];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult
                                                resultWithStatus:CDVCommandStatus_ERROR
                                                messageAsString:@"Device not supported."]
                                    callbackId:self.command.callbackId];
    }
    
}

#pragma mark - UIDocumentMenuDelegate
-(void)documentMenu:(UIDocumentMenuViewController*)documentMenu didPickDocumentPicker:(UIDocumentPickerViewController*)documentPicker {
    
    documentPicker.delegate = self;
    documentPicker.modalPresentationStyle = UIModalPresentationFullScreen;
    [self.viewController presentViewController:documentPicker animated:YES completion:nil];
    
}

-(void)documentMenuWasCancelled:(UIDocumentMenuViewController*)documentMenu {
    
    self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"canceled"];
    [self.pluginResult setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:self.pluginResult callbackId:self.command.callbackId];
    
}

#pragma mark - UIDocumentPickerDelegate
- (void)documentPicker:(UIDocumentPickerViewController*)controller didPickDocumentAtURL:(NSURL*)url {
    
    if (self.returnWithDetail) {
        NSArray* details = [self fileDetailsFromUrl:url];
        
        self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:details];
    } else {
        self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[url path]];
    }
    
    [self.pluginResult setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:self.pluginResult callbackId:self.command.callbackId];
    
}
- (void)documentPickerWasCancelled:(UIDocumentPickerViewController*)controller {
    
    self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"canceled"];
    [self.pluginResult setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:self.pluginResult callbackId:self.command.callbackId];
    
}

- (void)displayDocumentPicker:(NSArray*)UTIs {
    
    UIDocumentMenuViewController *importMenu = [[UIDocumentMenuViewController alloc] initWithDocumentTypes:UTIs inMode:UIDocumentPickerModeImport];
    importMenu.delegate = self;
    importMenu.popoverPresentationController.sourceView = self.viewController.view;
    [self.viewController presentViewController:importMenu animated:YES completion:nil];
    
}

#pragma mark - Utils
- (NSArray*)fileDetailsFromUrl:(NSURL*)url {
    // see: http://stackoverflow.com/questions/25520453/ios8-uidocumentpickerviewcontroller-get-nsdata
    [url startAccessingSecurityScopedResource];
    
    __block NSData *data;
    
    NSFileCoordinator *coordinator = [[NSFileCoordinator alloc] init];
    NSError *error;
    [coordinator coordinateReadingItemAtURL:url options:0 error:&error byAccessor:^(NSURL *newURL) {
        data = [NSData dataWithContentsOfURL:url];
    }];
    
    [url stopAccessingSecurityScopedResource];
    
    NSString* urlString = [url absoluteString];
    NSString* fileNameWithType = [urlString substringFromIndex:[urlString rangeOfString:@"/" options:NSBackwardsSearch].location + 1];
    NSInteger fileTypeStartIndex = [fileNameWithType rangeOfString:@"." options:NSBackwardsSearch].location;
    
    NSString* fileName;
    NSString* fileType;
    
    if (fileTypeStartIndex <= [fileNameWithType length]) {
        fileName = [[fileNameWithType substringToIndex:fileTypeStartIndex] stringByReplacingOccurrencesOfString:@"%20" withString:@" "];
        fileType = [fileNameWithType substringFromIndex:fileTypeStartIndex+1];
    } else {
        fileName = [fileNameWithType stringByReplacingOccurrencesOfString:@"%20" withString:@" "];
        fileType = @"";
    }
    
    return [NSArray arrayWithObjects:[data base64EncodedStringWithOptions:0], fileName, fileType, nil];
}
@end
