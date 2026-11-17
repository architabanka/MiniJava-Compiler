# Build the MacroJava -> MiniJava translator.

CC     ?= cc
FLEX   ?= flex
BISON  ?= bison
LEXLIB ?= $(shell test "$$(uname -s)" = Darwin && echo -ll || echo -lfl)

A1.out: A1.l A1.y
	$(BISON) -d A1.y
	$(FLEX) A1.l
	$(CC) A1.tab.c lex.yy.c $(LEXLIB) -o $@

clean:
	rm -f *~ *.c *.out *.exe *.tab.h *.tab.c lex.yy.c

.PHONY: clean

