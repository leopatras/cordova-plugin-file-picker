/*
 * FilePicker.m
 *
 * Created by @jcesarmobile
 * Edited by @elizabethrego
 */

#import "FilePicker.h"

/**
 * This plugin allows a user on iOS > 7 to pick a file from
 * their device.
 */
@implementation FilePicker

/**
 * Returns true for devices on iOS > 7 and false otherwise.
 * Used to check if a document picker can be opened before 
 * that is attempted.
 * 
 * @param {CDVInvokedUrlCommand*} command
 *        The command sent from JavaScript
 */
- (void)deviceSupported:(CDVInvokedUrlCommand*)command {
    BOOL supported = NO;

    // If the class is found, device is on iOS > 7
    if (NSClassFromString(@"UIDocumentPickerViewController")) {
        supported = YES;
    }
    
    [self.commandDelegate sendPluginResult:[CDVPluginResult
                                            resultWithStatus:CDVCommandStatus_OK
                                            messageAsBool:supported]
                                callbackId:command.callbackId];
}

/**
 * Configures settings for and makes a call to display document
 * picker.
 * 
 * @param {CDVInvokedUrlCommand*} command
 *        The command sent from JavaScript
 */
- (void)pickFile:(CDVInvokedUrlCommand*)command {
    
    self.command = command;

    // UTIs are identifiers for types of documents that may be picked
    id UTIs = [command.arguments objectAtIndex:0];

    // To return with detail means to return the base 64 string representation
    // of the file rather than its URL, along with it's name and file type
    self.returnWithDetail = [[command.arguments objectAtIndex:1] boolValue];
    
    BOOL supported = YES;
    
    NSArray* UTIsArray = nil;
    if ([UTIs isEqual:[NSNull null]]) {
        // Default UTI allows all file types
        UTIsArray =  @[@"public.data"];
    } else if ([UTIs isKindOfClass:[NSString class]]){
        UTIsArray = @[UTIs];
    } else if ([UTIs isKindOfClass:[NSArray class]]){
        UTIsArray = UTIs;
    } else {
        supported = NO;
    }
    
    // If the class is not found, device is on iOS <= 7
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

#pragma mark - UIDocumentPickerDelegate
/**
 * Retrieves URL of picked document and sends it off for processing if
 * necessary or returns it.
 * 
 * @param {UIDocumentPickerViewController*} controller
 *        Delegate for document picker
 * @param {NSURL*} url
 *        URL of the picked document
 */
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

/**
 * Notifies when document picker was exited without file selection.
 * 
 * @param {UIDocumentPickerViewController*} controller
 *        Delegate for document picker
 */
- (void)documentPickerWasCancelled:(UIDocumentPickerViewController*)controller {
    self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"canceled"];
    [self.pluginResult setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:self.pluginResult callbackId:self.command.callbackId];
}

/**
 * Presents the actual document picker.
 * 
 * @param {NSArray*} UTIs
 *        Array of Uniform Type Identifiers specifying
 *        types that are allowed to be picked
 */
- (void)displayDocumentPicker:(NSArray*)UTIs {
    UIDocumentPickerViewController *importMenu = [[UIDocumentPickerViewController alloc] initWithDocumentTypes:UTIs inMode:UIDocumentPickerModeOpen];
    importMenu.delegate = self;
    importMenu.popoverPresentationController.sourceView = self.viewController.view;
    [self.viewController presentViewController:importMenu animated:YES completion:nil];
}

#pragma mark - Utils
/**
 * Gets details for file picked with returnWithDetails flag.
 * 
 * @param {NSURL*} url
 *        URL of the picked document
 */
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
