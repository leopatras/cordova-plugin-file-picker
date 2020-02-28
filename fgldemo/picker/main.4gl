IMPORT util
IMPORT os
IMPORT FGL fgldialog

MAIN
  OPEN FORM f FROM "main"
  DISPLAY FORM f
  MENU
    COMMAND "PickImage"
      CALL pickFile()
    COMMAND "Exit"
      EXIT MENU
  END MENU
END MAIN

FUNCTION pickFile()
  DEFINE result,fname STRING
  DEFINE fileUTIs DYNAMIC ARRAY OF STRING
  DEFINE fileDetails DYNAMIC ARRAY OF STRING
  LET fileUTIs[1]="public.png"
  LET fileUTIs[2]="public.jpeg"
  LET fileUTIs[3]="public.heic"
  LET fileUTIs[4]="public.heif"
  LET fileUTIs[5]="com.compuserve.gif"
  TRY
  CALL ui.Interface.frontCall("cordova","call",
                             ["FilePicker","pickFile",fileUTIs,TRUE],[fileDetails])
     LET fname=os.Path.makeTempName()
     IF fileDetails.getLength()<3 THEN
       ERROR "fileDetails must have length 3"
     ELSE
       --first data,then fileName, then fileType
       --obviously this should have been returned better as a dictionary
       ERROR "fileName:",fileDetails[2],",type:",fileDetails[3],",tmp:",fname
       CALL util.Strings.base64Decode(fileDetails[1],fname)
       DISPLAY fname TO img
     END IF
  CATCH 
     ERROR err_get(status)
  END TRY
END FUNCTION
