#
# Makefile to build the polyglot source to source compiler
# includes a makefile in each package to handle building of respective 
# packages
#

# set up some reasonable defaults (for building in CUCS)
include $(SOURCE)/Defs.mk

.SUFFIXES: .class .java

CC			= gcc
JC_FLAGS 		= -g -d $(OUTPUT) $(JAVAC_PATHS) -deprecation
RMIC_FLAGS		= -d $(OUTPUT) -classpath $(CLASSPATH)

JAR_FLAGS		= cf 

JAVADOC_MAIN		= com.sun.tools.javadoc.Main
JAVADOC_DOCLET		= iContract.doclet.Standard
JAVADOC_OUTPUT		= $(SOURCE)/javadoc
JAVADOC_FLAGS		= -mx40m -ms40m -classpath "$(JAVADOC_CLASSPATH)" -sourcepath "$(JAVADOC_SOURCEPATH)"

BIN 			= $(SOURCE)/bin
JIF			= $(BIN)/jifc
JIF_FLAGS		= -d $(OUTPUT) -sourcepath $(BUILDPATH)

SOURCEPATH		= $(SOURCE)
PACKAGEPATH		= $(SOURCE)/classes/$(PACKAGE)
VPATH			= $(PACKAGEPATH)
RELPATH			= $(SOURCE)/release/jif
REL_DOC			= $(RELPATH)/doc
REL_IMG			= $(RELPATH)/images
REL_LIB			= $(RELPATH)/lib
REL_SOURCES		= $(SOURCES)

# To avoid repeated slashes
DIR_ = $(DIR)
PACKAGE_ = $(PACKAGE)
DEMO_DIR_ = $(DEMO_DIR)
ifneq ($(DIR)foo, foo) 
DIR_ = /$(DIR)
endif
ifneq ($(PACKAGE)foo, foo)
PACKAGE_ = /$(PACKAGE)
endif
ifneq ($(DEMO_DIR)foo, foo)
DEMO_DIR_ = /$(DEMO_DIR)
endif
REL_SRC = $(RELPATH)/src$(DIR_)$(PACKAGE_)
REL_DEMO = $(RELPATH)/demo$(DEMO_DIR_)$(PACKAGE_)

all clean clobber javadoc release:

$(PACKAGEPATH)/%.class: %.java
	$(JC) $(JC_FLAGS) $<

cleanclasses:
	-rm -f $(PACKAGEPATH)/*.class

clean_java_output:
	-rm -f $(PACKAGEPATH)/*.java*

classpath:
	@echo "setenv CLASSPATH $(CLASSPATH)"

release_files:
	@for i in $(REL_SOURCES); do echo $(REL_SRC)$$i; done
	echo $(REL_SRC)Makefile
	@if [ -f package.html ]; then echo $(REL_SRC)package.html; fi
	$(subdirs)

release_src:
	mkdir -p $(REL_SRC)
	@cp -f $(REL_SOURCES) Makefile $(REL_SRC)
	@if [ -f package.html ]; then cp package.html $(REL_SRC); fi

release_demo:
	mkdir -p $(REL_DEMO)
	@if [ -n "$(DEMOS)" ]; then cp -f $(DEMOS) $(REL_DEMO); fi
	@if [ -f package.html ]; then cp package.html $(REL_DEMO); fi

define subdirs
@for i in $(SUBDIRS) ""; do \
    if [ "x$$i" != "x" ]; then $(MAKE) -C $$i $@ || exit 1; fi; \
done
endef

# define javadoc
# -rm -rf $(JAVADOC_OUTPUT)
# -mkdir -p $(JAVADOC_OUTPUT)
# "$(JAVA)" "$(JAVADOC_FLAGS)" $(JAVADOC_MAIN) \
# 	-d $(JAVADOC_OUTPUT) \
# 	-doclet $(JAVADOC_DOCLET) \
# 	-sourcepath $(SOURCEPATH) \
# 	-classpath "$(CLASSPATH)" $(JAVADOC_PACKAGES)
# endef

define javadoc
-rm -rf $(JAVADOC_OUTPUT)
-mkdir -p $(JAVADOC_OUTPUT)
$(JAVADOC) -d $(JAVADOC_OUTPUT) \
	-sourcepath $(JAVADOC_SOURCEPATH) \
	-classpath "$(CLASSPATH)" \
	$(JAVADOC_PACKAGES)
endef
#	-doclet $(JAVADOC_DOCLET) \
#	-docletpath "$(JAVADOC_CLASSPATH)" \

define yacc
awk 'BEGIN {FS = "\n"; s = 1} {print $$1, "\t // ", s++}' $< | \
	$(JAVA) -classpath $(CLASSPATH) java_cup.Main -parser Grm
endef

define flex
	$(JAVA) -classpath $(CLASSPATH) JFlex.Main $<
endef


define ppg
	$(JAVA) -classpath $(CLASSPATH) polyglot.util.ppg.PPG
endef

define jar
	@cd $(SOURCE)/classes; \
	for f in $(JAR_FILE); do \
	    x=""; \
	    for c in $(PACKAGE)/*.class; do \
	        if [ -f $$c ]; then x="$$x $$c"; fi; \
	    done; \
	    if [ ! -z "$$x" ]; then \
		if [ -f ../$$f ]; then \
		    echo jar uf ../$$f $$x; \
		    jar uf ../$$f $$x; \
		else \
		    echo jar cf ../$$f $$x; \
		    jar cf ../$$f $$x; \
		fi; \
	    fi; \
	done
endef

jar: all
	$(jar)
	$(subdirs)
