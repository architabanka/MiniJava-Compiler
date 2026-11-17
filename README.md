# MiniJava to MIPS Optimizing Compiler

An end-to-end, multi-stage optimizing compiler that progressively lowers **MacroJava / MiniJava** code into executable **MIPS assembly** (executable in SPIM / MARS simulators).

Built as part of CS3300 (Compiler Design) at IIT Madras.

---

## 🏗️ Compiler Architecture & Pipeline

The compilation pipeline is structured into six discrete passes:

```
 MacroJava Source Code
        │
        ▼  [Stage 1: Lex & Yacc]
 MiniJava Source Code
        │
        ▼  [Stage 2: Type Checker (JavaCC / JTB)]
 Type-Safe MiniJava AST
        │
        ▼  [Stage 3: Intermediate Code Generation]
 MicroIR
        │
        ▼  [Stage 4: Canonicalization & Flattening]
 MiniIR
        │
        ▼  [Stage 5: Liveness Analysis & Linear Scan Allocation]
 MiniRA (Register-Allocated IR)
        │
        ▼  [Stage 6: Code Generation & Stack Frame Layout]
 Executable MIPS Assembly (.s)
```

### Pipeline Overview:

1. **`01_macro_to_minijava` (Lexer & Parser in Flex/Bison):**
   - Parses MacroJava macros (statement and expression macros).
   - Performs macro expansion and outputs clean, syntactically valid MiniJava.
2. **`02_typechecker` (Static Semantic Analysis):**
   - Two-pass AST traversal (using JTB Visitor pattern).
   - Constructs symbol tables, resolves class hierarchies, inheritance, and method overriding.
   - Enforces static type checking rules (type compatibility, scoping, variable initialization).
3. **`03_minijava_to_microIR` (Intermediate Code Generation):**
   - Lowers high-level Object-Oriented MiniJava abstractions into MicroIR.
   - Converts dynamic dispatch and class instantiation into explicit heap allocations (`HALLOCATE`) and Virtual Method Tables (VMTs).
4. **`04_microIR_to_miniIR` (IR Canonicalization):**
   - Flattens nested expressions, canonicalizes complex memory operations, and standardizes statement sequences.
5. **`05_miniIR_to_miniRA` (Liveness Analysis & Register Allocation):**
   - Computes live intervals for all virtual temporaries across basic blocks.
   - Implements **Linear Scan Register Allocation** across standard MIPS registers (`$s0-$s7`, `$t0-$t9`).
   - Generates spill slots on the activation record with load/store (`ALOAD`/`ASTORE`) traffic minimization.
6. **`06_miniRA_to_mips` (MIPS Assembly Code Generation):**
   - Generates production-ready MIPS assembly code conforming to standard MIPS calling conventions.
   - Manages activation record layout, callee-saved register persistence, and system calls (`_print`, `_halloc`).

---

## 🚀 Building & Running

### Prerequisites
- `Java JDK 8+`
- `flex` and `bison` (for Stage 1)
- `SPIM` or `MARS` (MIPS simulator)

### Running Each Stage

Each subfolder contains its own standalone `Makefile`.

#### Example: Stage 1 (MacroJava to MiniJava)
```bash
cd 01_macro_to_minijava
make
./A1.out < input.macrojava > output.minijava
```

#### Example: Stage 2 (Type Checker)
```bash
cd 02_typechecker
make
java P1 < input.minijava
# Outputs: Program type checked successfully
```

#### Example: Stage 6 (MiniRA to MIPS) & Execution in SPIM
```bash
cd 06_miniRA_to_mips
make
java P5 < input.miniRA > output.s
spim -file output.s
```

---

## 🛠️ Tech Stack & Tools
- **Languages:** Java, C
- **Compiler Construction Tools:** Flex, Bison, JavaCC, JTB (Java Tree Builder)
- **Target Architecture:** MIPS32 Assembly (SPIM simulator)
