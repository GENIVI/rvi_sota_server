TARGETS = images/Data-Model.png images/System-Architecture-Diagram.png
LARGE_TARGETS = images/Data-Model-large.png

all: $(TARGETS) $(LARGE_TARGETS)

images/%.png: ../architecture/%.svg
	inkscape --export-png=$@ --export-width=800 --export-background=WHITE $<

images/%-large.png: ../architecture/%.svg
	inkscape --export-png=$@ --export-width=1600 --export-background=WHITE $<

clean: $(TARGETS) $(LARGE_TARGETS)
	rm -f $^

.PHONY: all clean
