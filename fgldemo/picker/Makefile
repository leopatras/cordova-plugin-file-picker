BUNDLE_IDENTIFIER   = com.fourjs.filepicker
APPNAME             = FilePicker
DEVICE              = phone
MODE                = debug
VERBOSE             = no
IDENTITY='(59W7RY75ZU)'
PROVISIONING_PROFILE=/Users/leo/Library/MobileDevice/Provisioning\ Profiles/filepicker
FORMS   = $(patsubst %.per, %.42f, $(wildcard *.per))

PICKER=cordova-plugin-file-picker
# in case you build the demo when having the plugin checked out of github 
# then we use the fglcvdMedia.42m in the fgl dir of the plugin
ifneq ($(wildcard ../../fgldemo),)
  PICKER=$(CURDIR)/../..
#  FGLLDPATH=../../fgl
endif

MODULES  = $(patsubst %.4gl, %.42m, $(wildcard *.4gl))
FORMS    = $(patsubst %.per, %.42f, $(wildcard *.per))

%.42m: %.4gl
#	FGLLDPATH=$(FGLLDPATH) fglcomp -M --resolve-calls $*
	fglcomp -M --resolve-calls $*

%.42f: %.per
	fglform -M $*

all:: $(MODULES) $(FORMS)

main.42m: main.42f 

run: main.42m
#	FGLLDPATH=$(FGLLDPATH) fglrun main
	FGLLDPATH=$(FGLLDPATH) fglrun main

define gmibuild
	gmibuildtool --install=$(1) --verbose=$(VERBOSE) --app-name=$(APPNAME) --bundle-id=$(BUNDLE_IDENTIFIER) --provisioning=$(PROVISIONING_PROFILE) --certificate=$(IDENTITY) --device=$(DEVICE) --mode=$(MODE) --build-cordova=$(PICKER)
endef

gmi.build: all
	$(call gmibuild,no)

gmi.install: all
	$(call gmibuild,yes)

#always open the Xcode project with "make openxcode"!
openxcode:
	@rm -f gmi.env
ifdef GMIDIR
	echo "export GMIDIR=$(GMIDIR)" > gmi.env
endif
ifdef FGLDIR
	echo "export FGLDIR=$(FGLDIR)" >> gmi.env
else
	$(error FGLDIR must be set)
endif
	rm -f ./fgldir
	ln -s $$FGLDIR ./fgldir
	open picker.xcodeproj

clean::
	rm -f *.sch *.dbs *.42*
	rm -rf build bin distbin
	rm -f cordovamap.txt gmi/*.a gmi/link_flags.sh *.4pwvs *.4pwdb ../../src/ios/*.a
	rm -f ../../pluginCalls.*

distclean::clean
	rm -rf gmi.env picker.xcodeproj/project.xcworkspace/ picker.xcodeproj/xcuserdata
	rm -f ./fgldir
