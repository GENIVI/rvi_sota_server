TARGETS = $(shell pwd)/images/Data-Model.png $(shell pwd)/images/System-Architecture-Diagram.png
LARGE_TARGETS = $(shell pwd)/images/Data-Model-large.png

all: $(TARGETS) $(LARGE_TARGETS)

$(shell pwd)/images/%.png: $(shell pwd)/../architecture/%.svg
	inkscape --export-png=$@ --export-width=800 --export-background=WHITE $<

$(shell pwd)/images/%-large.png: $(shell pwd)/../architecture/%.svg
	inkscape --export-png=$@ --export-width=1600 --export-background=WHITE $<

clean: $(TARGETS) $(LARGE_TARGETS)
	rm -f $^

.PHONY: all clean
