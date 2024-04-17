+++
title = "Evolution of a Scala Intcode interpreter"
description = "A post following the changes to a Scala solution for the Advent of Code 2019 Intcode series of challenges"
date = 2024-04-03

[taxonomies]
tags = ["Advent Of Code", "Intcode", "Interpreters", "Scala"]

[extra]
keywords = "Algorithms, Advent of Code, Intcode, bytecode, interpreters, VM, Scala"
toc = true
series = "Advent Of Code"
+++

This post follows the changes to a Scala interpreter used during the [Advent of Code 2019](https://adventofcode.com/2019) Intcode series of challenges and shows how taking advantage of a programming language's features can make it easier to adapt to evolving requirements.

<!-- more -->


## Overview

Quoting [Wikipedia](https://en.wikipedia.org/wiki/Advent_of_Code), "Advent of Code is an annual set of Christmas-themed computer programming challenges that follow an Advent calendar". A new puzzle is made accessible every day at midnight EST, consisting of two parts. Participants can solve them using whatever programming language or method they choose, as long as they find the correct output for the input provided to them by the site. The first part one is a simpler puzzle. Solving it unlocks the second part, which is usually a more elaborate or larger-scale version of the first. Participants are shown their ranking in a global leaderboard based on their completion time per part, counted from the time the puzzle was published. Private leaderboards are also possible.

[Intcode](https://esolangs.org/wiki/Intcode) is a bytecode specification for an abstract von Neumann architecture that was gradually built up and used in 12 of the 25 programming challenges during the 2019 edition of Advent of Code. It represented a departure from the usual self-contained AoC puzzles, which enjoyed a mixed reception, but allowed for bigger scope and rewarded reusable code designs.

The sections below follow the development of an interpreter for Intcode written in Scala. Due to the nature of AoC, the main priority during the creation of the below solutions was to use the fastest and most obvious (to the author) approach to get the correct output. Completely separate and self-contained source code files were used to allow maximum flexibility of the reused code (at the cost of needing to backport fixes, which is a much smaller concern in this situation). The approaches used can include exhaustive searching if the solution space is small enough, or even partial manual calculation, when the total time to reach a correct solution with the programmatic one would be significantly slower. Further, the website serves as an oracle (that is rate-limited, but not overly so), so certainty in the correctness of the output is not the highest priority - a lower time to success can often be achieved with some minimal trial and error.

As a result, this series does not aim to show the best or most sophisticated solutions, but to serve as an experience log that includes some observations on fitness of programming paradigms for different situations.

<mark>SPOILER WARNING:</mark> in Advent of Code, a noteworthy part of the experience is reading the story ("flavor text") that serves as a backdrop for these puzzles, as well as deciphering what it boils down to. The following sections contain short summaries for many of those, which may spoil your experience if you haven't already solved them.


## Day 2: Hello Intcode
### d2p1: add, multiply, halt {#d2p1}

Summarizing the [problem statement](https://adventofcode.com/2019/day/2), we are given a comma-separated line of integers that represent an Intcode program. The Intcode specification is:

* the first integer is the opcode - either `1`, `2` or `99`
* opcode `1` is addition, taking the next three integers as its operands
    * all three operands' values represent 0-based positions within the Intcode program
    * the values at the positions pointed to by the first two operands' values are added
    * the result is stored at the position pointed to by the third operand's value
* opcode `2` is multiplication, the operands are the same as with addition
* opcode `99` is `halt`

Running an Intcode program involves starting at position 0, decoding and executing the instruction at the current position, once an `add` or `mul` instruction has been processed, advancing the position by 4, and repeating until we hit an unrecognized opcode or `halt`.

Our task is to patch the program—setting the values of positions `1` and `2` to `12` and `2`, respectively—and run it until it halts.

The output we need to submit is the value of position `0` after the program halts.

To solve this, we parse the input into an integer array (the "tape", as an homage to Turing machines) and run a simple decode-execute loop (`run(pos)`). For ease of use, we do this in a self-contained [Ammonite Scala 2 script](2/p1.sc):

{{code_block_of_file(path="2/p1.sc", info_string="sc, linenos")}}


### d2p2: find patch values {#d2p2}

The [problem statement](https://adventofcode.com/2019/day/2#part2) is only visible once we have solved [part one](#d2p1), but boils down to finding which two numbers (`noun` and `verb`) we need to use for patching instead of `12` and `2`, to make the program output `19690720`. The range for both is $[0;99]$, and the expected output format encodes them as a single number.

The fastest way to reuse our previous code is to
* parametrize the patching by moving the code into its own function (`isAnswer(noun, verb)`); we also make the function into a predicate and hard-code the value we're looking for, so we can most easily use the short-circuiting `find()`
* use that function to search the entire search space and print the first match

`diff -wu10 p1.sc p2.sc | tail +6` shows how we "wrap" the code from part 1:

{{code_block_of_file(path="2/p1p2.diff", info_string="diff")}}

And the [complete source code](2/p2.sc) is:

{{code_block_of_file(path="2/p2.sc", info_string="sc, linenos")}}


## Day 5: Intcode expanded
### d5p1: IO, parameter modes {#d5p1}

In [day 5](https://adventofcode.com/2019/day/5), two new opcodes are added:

* `3` takes an integer as input and saves it to the position given by its parameter
* `4` outputs the value of the position given by its parameter

Parameter modes are also introduced:

* the first integer in each instruction can be interpreted as a zero-expanded five-digit number that encodes the following (where `op` stands for "operand") :
    * `op3mode`, `op2mode`, `op1mode`, `twoDigitOpcode`
* there are two parameter modes we need to handle
    * `0` is position mode, which causes the parameter to be interpreted as a position (address) within the program. Position mode parameters are thus somewhat similar to [lvalues](https://eli.thegreenplace.net/2011/12/15/understanding-lvalues-and-rvalues-in-c-and-c) in that they have an address and can be written to.
    * `1` is immediate mode, causing a parameter to be interpreted as a value. Instructions will never write to parameters in immediate mode, making those similar to rvalues.

Our input is an Intcode program, and our task is to execute it, providing `1` for input. It performs "a series of diagnostic tests confirming that various parts of the Intcode computer, like parameter modes, function correctly", outputting `0` for each successful one. Finally, it outputs a diagnostic code and halts. The output we need to submit is that diagnostic code.

At this point it is worth it to start introducing some organization into the code:

* operands become a case class with a `get` method to deal with parameter modes
* instructions are reified into an `Insn` sum type to ease debugging
* as the decode-execute loop now has more end-states than simply halting, we introduce a `State` sum type to encode those, making the loop stop at any of `Halted`, `Output(v: Int)` or `NeedInput`
* we wrap the decode-execute loop and state (tape and instruction pointer) in the `Proc` case class, which makes a copy of the tape provided at object construction time
    * we split the decode and execute parts, adding another entry point—`stepWithInput(v: Int)`—to handle the input instruction

This produces the following [code](5/p1.sc):

{{code_block_of_file(path="5/p1.sc", info_string="sc, linenos")}}


### d5p2: jump, compare {#d5p2}

Part two introduces branching and conditional instructions with the following opcodes:

* `5` is `jump-if-true`. If the first parameter is non-zero, it sets the instruction pointer to the value of the second parameter, otherwise doing nothing
* `6` is `jump-if-false`, setting the instruction pointer only when the first parameter is zero
* `7` is `less than`, storing `1` in the position given by the third parameter, if the value of the first parameter is smaller than the value of the second parameter
* `8` is `equals`, acting similarly, but only when the value of the first parameter is equal to the value of the second parameter

Additionally, the instruction pointer should not advance after it has been modified by an instruction.

Our task is to run the provided program again, this time providing a `5` for input. It outputs only one diagnostic code, which is our result.

To implement this, we just add the necessary variants to our `Insn` type, and handle those appropriately in the `decode` and `step` functions. We also add an `UnexpectedInput` state for debugging purposes, and rename position-mode-only parameters to `p`, resulting in the [complete source code](5/p2.sc) being:

{{code_block_of_file(path="5/p2.sc", info_string="sc, linenos")}}


## Day 7: Multi-process
### d7p1: chained {#d7p1}

[Day 7](https://adventofcode.com/2019/day/7) introduces no changes to the Intcode specification, but instead is the first puzzle to require running multiple independent copies of the provided program, feeding the output of one to the input of another.



There are five amplifiers, connected in series. Independent copies of the program run on each one. Upon starting the program, it requires a `phase setting` to be provided. This is an integer from `0` to `4`. Each one may be used exactly once, and we don't know which setting is fed to which amplifier.

Then, the program reads the input signal. For the first amplifier that is `0`, provided by us. Afterwards, it uses the output instruction to provide the output signal. This is fed to the next amplifier, repeating the process until the last one provides its output.

Our task is to find which permutation of phase settings produces the maximum output from the last amplifier.

The ease of implementation of these features in Scala has already guided us to use independent copies of the program data in [day 2, part 2](#d2p2), and to encapsulate all process state in [day 5, part 1](#d5p1). This allows us to leave the interpreter completely unchanged—excluding a renaming of the `JIT` and `JIF` instructions to `JNZ` and `JZ` to be more consistent with existing opcode names in other ISAs—and attempt a simple exhaustive search implementation. We do this via a `runAmp()` helper function to massage input and output, wrapping that in a `run()` function to test each possible phase setting permutation, resulting in the following [code](7/p1.sc):

{{code_block_of_file(path="7/p1.sc", info_string="sc, linenos")}}

This turned out to be sufficiently fast (26 ms), so priority was given to solving part two.

### d7p2: feedback {#d7p2}

Part two introduces feedback. First, the phase settings are now between `5` and `9`. The last amplifier's output is fed back to the first one, continuing until the program on the amplifiers halts. Besides those changes, the process and our task remain unchanged.

To implement this, we split the phase setting setup and the amp signal processing, wrapping the latter in a loop that breaks at the appropriate moment, resulting in [a solution](7/p2.sc) that executes in 27 ms. The interpreter remains completely unchanged, so only the modified part of the source code is shown below:

{{code_block_of_file(path="7/p2.sc", info_string="sc, linenos, hide_lines=1-62")}}


## Day 9: Scaling memory
### d9p1: long, resize, rel-mode {#d9p1}

[Day 9](https://adventofcode.com/2019/day/9) expands the Intcode specification in three ways:

* Support for large numbers. The problem statement includes example programs that output 16-digit numbers, meaning we'll need at least 54 bits to represent each.
* Support for positions beyond those in the original program. Reading from uninitialized addresses should return 0. Negative addresses are not allowed.
* Support for relative parameter mode. This is a position mode again, but not counted from `0`, but from a "relative base". This starts at `0`, and may be added to or subtracted from via a new opcode.

Our task after implementing those changes is simple: run the provided program, giving it `1` as input, and submit the one value that it outputs.

Implementing this triggered a refactoring, as parameter mode handling was deemed to be now too clunky. This included:
* abstracting over the tape element type, so we can easily switch that to `Long` or `BigInt` as necessary
* wrapping the tape into its own class to allow for automatic resizing when accessing a random address. An alternative would be to switch to an associative container, but the exponentially resized array worked well enough
* adding the relative parameter mode, triggering refactoring of the operand handling code, including taking advantage of subtyping to express the relationship between rvalues and lvalues
* adding support for the new opcode

This resulted in the following [code](9/p1.sc):

{{code_block_of_file(path="9/p1.sc", info_string="sc, linenos")}}


### d9p2: scaled up {#d9p2}

Our task is unchanged, except the input value is now `2`. The problem statement suggests this might result in a multi-second execution time on older hardware. Using this interpreter and execution environment, both parts take 11 ms each.

Naturally, the solution requires only a single-line change to the code, but this was taken as an opportunity to do some refactoring, including

* more terse names for operands and their types to reduce clutter in the decode-execute loop,
* addition of the `tailrec` annotation to ensure the decode-execute loop is tail-call optimized,
* minor variable naming improvements,

resulting in the following [code](9/p2.sc):

{{code_block_of_file(path="9/p2.sc", info_string="sc, linenos")}}

Another point of note is that the problem statement starts with "You now have a complete Intcode computer". The following days confirm that this marks the last place where the specification is changed.

## Day 11: Hull painting robot
### d11p1: painted set size {#d11p1}

In [day 11](https://adventofcode.com/2019/day/11), we need to paint a registration identifier on the hull of the spaceship we are in. All the hull panels are currently black. Our input is an Intcode program that can run a hull painting robot (a version of [Langton's Ant](https://en.wikipedia.org/wiki/Langton%27s_ant)). The program's main loop is as follows:

* we feed it `0` if the robot is over a black panel, or `1` if it's over a white one
* it outputs 2 values:
    * the color it will paint the panel with: `0` for black and `1` for white
    * the direction the robot will turn to after painting: `0` for a 90-degree left turn and `1` for a 90-degree right turn
* the robot then moves forward one panel

Once the robot is done painting, the program halts. The robot starts facing up.

Our task is to run the program and count how many panels it has painted at least once.

We [implement](11/p1.sc) this via a direct translation of the main loop above into a while loop that inspects the robot's response. We keep track of the hull panel colors in a `Map` with a default value, and print its size after the program has halted. Notably, we also revert the operand type renaming in the interpreter, as that was deemed be too terse in retrospect:

{{code_block_of_file(path="11/p1.sc", info_string="sc, linenos")}}

### d11p2: read letters {#d11p2}

In this part, we find out that there is a single white panel on the hull, and we need to start the robot from there. Our task is to run the program again, but this time we need to submit the 8 letter identifier the robot has painted.

The easiest way to achieve this is to visualize the hull after the program halts, and do manual optical character recognition. To make debugging easier, we reify the colors, moves and robot states into their own types, adjusting the loop to use those. The only other changes are setting the initial hull panel to `White`, and adding a hull visualization loop after the main one, resulting in the following [code](11/p2.sc) (omitting the unchanged interpreter):

{{code_block_of_file(path="11/p2.sc", info_string="sc, linenos, hide_lines=1-90")}}

This code organization was also chosen to allow us to quickly (using a two-line adjustment to the source code and a dozen-line shell script) create an animation of the hull and robot like the one below, which can be useful for visualizing its behavior and debugging.

<!-- TODO: see if [gif shortcodes](https://abridge.netlify.app/overview-rich-content/#gif) can work in a `zola serve` context -->

<img src="11/p2.withMoves.gif" />


## Day 13: Breakout arcade
### d13p1: count blocks {#d13p1}

In [day 13](https://adventofcode.com/2019/day/13), our input program is a [Breakout](https://en.wikipedia.org/wiki/Breakout_(video_game)) clone that runs on an arcade cabinet. It draws the screen by producing a stream of output instructions. Every three subsequent output values can be interpreted as the distance from the left edge of the screen, distance from the top, and a `tile id`, which is one of:

* `0` - an empty tile
* `1` - an indestructible wall
* `2` - a block that can be broken by the ball
* `3` - the horizontal paddle, which is also indestructible
* `4` - the ball, which moves diagonally and bounces off objects

Our task is to run the program and count how many block tiles are on the screen when it exits.

A quick implementation to get the correct output is to make a `step` function that extracts the `tile id` from every three output values, and count how many of those are `2`. The [code](13/p1.sc) is shown below (omitting the unchanged interpreter):

{{code_block_of_file(path="13/p1.sc", info_string="sc, linenos, hide_lines=1-90")}}


### d13p2: beat the game {#d13p2}

Part two explains that the game didn't run completely, because we didn't insert any quarters, and we need to set address `0` to `2` to play for free. During the real game loop, the program asks for the joystick position using input instructions. The values we can provide are:

* `-1`, if the joystick is tilted to the left
* `0`, if the joystick is in the neutral position
* `1`, if the joystick is tilted to the right

In addition, the program drives a segment display to keep the current score. It does that via screen drawing instructions that specify the `X=-1, Y=0` position via the first two values, and the current score as the third value.

Our task is to beat the game by breaking all the blocks, and submit the score that is shown afterwards.

The easiest strategy to follow here is to always move the joystick in the direction of the ball. Implementing the changes from part one,

* we patch the program at the start,
* we now need to keep track of the actual state of the screen, which we do with a `Map[Point,Num]`; we also keep a separate variable for the score,
* we then wrap the screen drawing loop from part 1 in a game loop: let the program draw the screen, provide joystick input, loop, exiting once all blocks are broken.

Normally we wouldn't need changes to the interpreter, but the input program has the right structure to trigger an issue in it: we don't have an entry point to process a single input instruction without continuing with the rest of the program. This highlights the misleading naming of the two entry points, as they "run" the program once called, even if they technically are recursive "step" functions. Thus, we adjust the naming of one entry point, and make the other execute a single instruction. This change and the time it took to detect the initial issue demonstrate some of the smaller risks of suboptimal naming. Note that as this part of day 13 was written on day 22, the interpreters in subsequent days do not benefit from it.

The resulting [code](13/p2.sc) is below:

{{code_block_of_file(path="13/p2.sc", info_string="sc, linenos")}}

## Day 15: Oxygen maze
### d15p1: shortest path {#d15p1}

In [day 15](https://adventofcode.com/2019/day/15), the oxygen system has failed in a part of our spaceship. Our input program controls a repair droid which we can use to locate the oxygen system. The program's loop consists of the following steps:

* receive movement command via input instruction
* execute that command
* report the current status via an output instruction

The possible movement commands are:

* `1` to move one step north
* `2` to move one step south
* `3` to move one step west
* `4` to move one step east

The possible status responses are:

* `0` if the droid has encountered a wall and not changed its location
* `1` if the move was successful and the robot's location is now one step further in the requested direction
* `2` is the same as `1`, and the oxygen system is at the new location

Our task is to find the minimum number of movement commands to move from the start location to the location of the oxygen system.

A direct approach would be to implement some backtracking or maze-traversing algorithm to find the oxygen system. However, a bit of lateral thinking can highlight that we have complete control of the maze and robot simulation and a generous resource envelope. By using [checkpointing](https://en.wikipedia.org/wiki/Application_checkpointing) in a manner similar to [savescumming](https://en.wikipedia.org/wiki/Saved_game#Savescumming), we can use the program as an [oracle](https://en.wikipedia.org/wiki/Category:Computation_oracles) to map out the maze and sidestep the need for explicit backtracking.

We implement the solution as a simple flood fill algorithm, using a recursive BFS function with a twist for finding the next wave of locations to explore: each element in the wave of currently traversed locations is a `PosProc` tuple of a location and a clone of the process that was used to reach it. With each of the current locations, we call `genNext` to create up to 4 new process clones—one for each direction, if the target hasn't been visited—and tell them to go in that direction, updating the maze with the answer. The ones that hit a wall are ignored, and the remaining are added to the next wave of locations to visit. We also keep track of the current distance from the start location, and we log it once we've found the oxygen system. Running this algorithm from the start location (designated $(0,0)$) with the input program provides us with the distance to the oxygen system.

The rest of the changes are adding 5 lines in the interpreter to support cloning, the usual sum types for directions, program responses and tiles, and the standard 2D maze and BFS support code. The complete [code](15/p1.sc) follows:

{{code_block_of_file(path="15/p1.sc", info_string="sc, linenos")}}


### d15p2: furthest location {#d15p2}

In part two, we repair the oxygen system, and it starts filling the area. It takes one minute to fill all locations adjacent to ones that already have oxygen.

Our task is to find how long it takes for the entire area to be filled with oxygen.

To solve this, we reuse the maze from part 1 and run another BFS, this time starting from the location of the oxygen system, keeping track of the maximum depth and printing it afterwards. The changes to the part 1 code are to encapsulate the `seen` set in the first BFS, and drop the no longer needed distance variable. We then add a straightforward `bfs2()` BFS implementation that returns the maximum traversal depth, find the location of the oxygen system in the maze and start `bfs2()` from it.

`diff -uw p1.sc p2.sc  | tail +6` shows the changes to the code,

{{code_block_of_file(path="15/p1p2.diff", info_string="diff")}}

resulting in the following [code](15/p2.sc) for part two (omitting the unchanged interpreter):

{{code_block_of_file(path="15/p2.sc", info_string="sc, linenos, hide_lines=1-99")}}

### d15viz: visualization {#d15viz}

By dumping the maze state as a [PPM](https://en.wikipedia.org/wiki/Netpbm#PPM_example) image at each BFS invocation ([full code](15/p2.anim.sc))

{{code_block_of_file(path="15/p2anim.diff", info_string="diff")}}

and using a simple [`csplit`](https://www.mankier.com/1/csplit)+ffmpeg script (including some nearest-neighbor upscaling and hardcoded dimensions)

{{code_block_of_file(path="15/mkwebm.sh", info_string="bash, linenos")}}

we can visualize the traversal

{{ video(sources=["15/aoc.2019.15.webm"]) }}

## Day 17: Scaffold traversal
### d17p1: find intersections {#d17p1}

In [day 17](https://adventofcode.com/2019/day/17), our spaceship has detected an incoming solar flare and activated its EM shield. This has cut off communication to many small robots on the exterior scaffolding that now need to be rescued. The only way to do that is via a vacuum robot, and our input is a program to control it. When it is run, it prints out the layout of the scaffold and the location and orientation of the robot via output commands returning the ASCII values of the characters `.#^v<>` and the number `10` (line feed), for example:

```txt
..#..........
..#..........
#######...###
#.#...#...#.#
#############
..#...#...#..
..#####...^..
```

Our task is to locate all scaffold intersections and return the sum of their `alignment parameters` (the product of their $x$ and $y$ coordinate).

To solve this, we run the program to create a string representation of the map and traverse it to build a set of all the scaffold locations. Using that, we find all the ones that are intersections, calculate their alignment parameters and print their sum. Omitting the unchanged interpreter, the [code](17/p1.sc) is below:

{{code_block_of_file(path="17/p1.sc", info_string="sc, linenos, hide_lines=1-99")}}


### d17p2: create program {#d17p2}

In the second part, we need to make the robot visit every part of the scaffold at least once. First, we need to wake it up by setting address `0` to `2`. Then, we need to provide it commands using the ASCII codes for:
* a line containing the main movement routine, which is a comma-separated sequence of `A`, `B` and `C`
* three lines, each describing one of the `A`, `B` and `C` movement functions used in the main movement routine. These are comma-separated sequences of
    * `L` or `R` to turn the robot 90 degrees to the left or right
    * a number of steps to move forward
* `y` or `n` and a newline to specify if we want to see a continuous video feed

Each line can be no longer than 20 characters, excluding the newline. The robot then executes the program, notifying all the robots on the scaffolding, finally outputting how much dust it has collected.

Our task is to figure out a valid program to input, run the program, and submit the output of the robot.

A future update to this post might include an automated solution for calculating the program, but after a short search for obvious solutions, and observing that many others did it manually, a manual approach was chosen for the sake of speed. This is described below:

First, we use [a subset of part one](17/printMap.sc) to print the map (omitting the unchanged interpreter):

{{code_block_of_file(path="17/printMap.sc", info_string="sc, linenos, hide_lines=1-99")}}

Which produces:

{{code_block_of_file(path="17/map.txt", info_string="txt")}}

In a text editor, we then manually generate the simplest command sequence to traverse the entire scaffold. Using five search and replace commands (in this case, `:%s` in vim), we indent different instances of the same command to the same horizontal offset. Using visual inspection, we then greedily separate the longest repeating sequences we can notice, visually fencing them off and trying to build the resulting program, splitting the sequences when they're too long to be a movement function. The above map resulted in the following:

{{code_block_of_file(path="17/moves.txt", info_string="txt")}}

We can then plug that program into the robot and print its output using the following [code](17/p2.sc) (omitting the unchanged interpreter):

{{code_block_of_file(path="17/p2.sc", info_string="sc, linenos, hide_lines=1-99")}}

<!--
Future work: automated move list generation. Possibly relevant:

* https://en.wikipedia.org/wiki/Balanced_number_partitioning
* https://en.wikipedia.org/wiki/Multiway_number_partitioning
* https://en.wikipedia.org/wiki/Bin_packing_problem
* https://en.wikipedia.org/wiki/Cutting_stock_problem
* linear partition problem in Skiena's intro to DP, https://www3.cs.stonybrook.edu/~algorith/video-lectures/1997/lecture11.pdf, possibly unrelated due to the repetition
* consider exhaustive search first (my input is 34 moves), then something fancier if priorities allow
-->

## Day 19: Tractor beam
### d19p1: count affected {#d19p1}

In [day 19](https://adventofcode.com/2019/day/19), we borrow a tractor beam and activate it, but we can't figure out if it is working. Our input program controls our drone system, which can deploy a drone to specific coordinates via two input instructions for the $x$ and $y$ coordinates, and output whether the drone is stationary (`0`) or being pulled by something (`1`).

Our task is to find how many points are affected by the tractor beam in the 50x50 area closest to the emitter.

An example in the puzzle demonstrates a 10x10 area near the emitter for visualization purposes:

```txt
       X
  0->      9
 0#.........
 |.#........
 v..##......
  ...###....
  ....###...
Y .....####.
  ......####
  ......####
  .......###
 9........##
 ```

The [brute-force solution](19/p1.sc) shown below (omitting the unchanged interpreter) is straightforward and sufficiently fast at under half a second, so in accordance with our priorities, we proceed directly to part two.

{{code_block_of_file(path="19/p1.sc", info_string="sc, linenos, hide_lines=1-99")}}

### d19p2: closest square {#d19p2}

In part two, we are to find the 100x100 axis-aligned square closest to the emitter that fits entirely within the tractor beam. We need to submit $10000*P_x + P_y$, where $P$ is the point closest to the emitter within that square.

The implementation scans down the beam until it finds a spot wide enough for our square, and then outputs the result. To do this,

* we start at the 100th [antidiagonal](https://en.wikipedia.org/wiki/Main_diagonal#Antidiagonal) (running from bottom-left to top-right), as that's guaranteed to be the first one where our square could fit - it has a 100-wide diagonal, and that's where the widest possible beam reaches that width;
* we assume that the beam is at least one square wide by then (this can fail for sufficiently narrow beams), and find the beam edges that intersect it;
* we then start stepping down the 101st, 102nd and so on antidiagonals, following the edges and checking if they now fit the square. At the moment they do, we have the location of our square (as the beam is no wider than $90\degree$), and we can easily calculate the coordinates of its top-left corner and print the result.

Omitting the unchanged interpreter, the source [code](19/p2.sc) is below:

{{code_block_of_file(path="19/p2.sc", info_string="sc, linenos, hide_lines=1-99")}}


## Closing thoughts

The Intcode series of puzzles turned out to be very enjoyable to write in Scala for multiple reasons:
* pattern matching, sufficiently terse syntax for algebraic data types, the standard FP vocabulary for collection manipulation, immutability by default, and an overall excellent standard library design make it a breeze to write and debug interpreters for languages of this complexity
* the ease of mixing in more traditional features such as subtyping and mutability (and the ease of keeping that contained) are the essence of picking the right tool for the job, allowing for more straightforward solutions to many of the problems in the puzzles
* the design choices in the syntax and libraries of the language make the path of least resistance usually the correct one in the longer term
* there were multiple points during the Intcode days when a new requirement turned out to already be implemented (multiple processes in [day 7](#d7p1)), or doable with a handful of lines (process cloning in [day 15](#d15p1))
* the usual advantages of a compiled language on a mature and performant execution platform (the JVM), that provides type inference giving a good amount of boilerplate reduction while still supporting subtyping, and generally finds a good balance between terseness and boilerplate

There naturally are some trade-offs. The code above is written in Scala 2, which has a somewhat verbose representation of sum types. Scala 3 [improves significantly](https://docs.scala-lang.org/scala3/reference/enums/enums.html) on that for simpler use cases, but it also drops support for implicits (used here since [day 9](#d9p1)) due to their [various downsides](https://docs.scala-lang.org/scala3/reference/contextual/index.html), and migration of unwrapped mutable implicit values to the new system is not straightforward, although that in itself can be argued to be a code smell. As an aside, to demonstrate some of the improvements in Scala 3, a future update to this post might include versions of the solutions migrated to it.

However, all things considered, small-scale programming for scenarios like Advent of Code and similar is a pleasure in Scala, often feeling like writing executable pseudocode.