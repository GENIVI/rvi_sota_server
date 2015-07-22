TARGETS = architecture/Data-Model.pdf architecture/System-Architecture-Diagram.pdf \
					security/dfds/context.pdf security/dfds/server_level1.pdf

all: $(TARGETS)

%.pdf: %.svg
	inkscape --export-pdf=$@ $<

clean: $(TARGETS)
	rm -f $^

.PHONY: all clean
