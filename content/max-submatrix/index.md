+++
title = "Maximum nonzero contiguous square submatrix"
description = "Combining summed-area tables and another dynamic programming pattern to solve a competitive programming problem."
date = 2023-03-20

[taxonomies]
tags = ["Algorithms","Dynamic programming","C++"]

[extra]
math = true
math_auto_render = true
keywords = "Algorithms, Dynamic programming, Competitive programming"
toc = true
series = "Algorithms"
+++


This post shows how to combine two *DP* ([dynamic programming](https://en.wikipedia.org/wiki/Dynamic_programming)) patterns—using a summed-area table, and taking advantage of a constraint in the problem statement—to solve a competitive programming problem in $\mathcal{O}(MN)$ time.


<!-- more -->


## Problem statement

Given a matrix $A$ of non-negative integers with dimensions $M{\times}N$, find the first *NzCSS* (nonzero contiguous square submatrix) with the largest sum of elements.

Constraints:

* $M \leq 5000$
* $N \leq 5000$
* $ \sum_{i, j} A_{ij} < 2^{64} $
* the matrix contains at least one non-zero integer


## Example

First, we'll generate the [input data](input.txt) we'll be using below:

```sh
$ ./gen_input.py 15 10 2038 11 --max_val 99 | column -t > input.txt
```

{{code_block_of_file(path="input.txt", info_string="txt")}}

The answer for which is a $5{\times}5$ region with an upper left corner at $(3,4)$ and a sum of $1209$:

```sh
$ ./cutRegion.sc 3 4 7 8 < input.txt | column -t
```

```txt
5   5
18  8   31  74  62
4   54  58  35  92
11  34  71  92  23
43  64  88  60  88
14  36  96  35  18
```

[`gen_input.py`](gen_input.py) and [`cutRegion.sc`](cutRegion.sc) can be found in the [Helper scripts](#helper-scripts) section below.

## Solution

Our approach can be roughly divided into three steps:

1. We use a summed-area table to be able to calculate the sum of any contiguous submatrix in $\mathcal{O}(1)$, after a $\mathcal{O}(MN)$ initialization step.

2. We take advantage of the constraint that there are no negative numbers to reduce the problem further: for each element $A_{ij}$, among all square submatrices that have it as a lower right corner, the widest one will have the largest sum. Thus, we can avoid processing all others, and are left with only $M{\times}N$ candidate submatrices to consider for the whole problem.

3. We use a similar DP approach to find the widest NzCSS with a lower right corner at $(i,j)$ in $\mathcal{O}(1)$, using those found for 3 of its immediate neighbors.


### Summed-area table

We generate a [summed-area table](https://en.wikipedia.org/wiki/Summed-area_table) (a 2D generalization of [prefix sums](https://en.wikipedia.org/wiki/Prefix_sum)) $S$, such that $S_{ij}$ is the sum of all elements in the contiguous submatrix from $A_{00}$ to $A_{ij}$ (inclusive), or

$$S_{ij} = \sum_{i^\prime=0,j^\prime=0}^{i,j} A_{i^\prime j^\prime}$$

Much like prefix sums, their 2D generalization obeys a recurrence relation, albeit a slightly more complicated one:

$$
S_{ij} = A_{ij} +
    \begin{cases}
        0                                   & \text{if } i=0, j=0     \\\\
        S_{i,j-1}                           & \text{if } i=0          \\\\
        S_{i-1,j}                           & \text{if } j=0          \\\\
        S_{i-1,j} + S_{i,j-1} - S_{i-1,j-1} & \text{otherwise}
    \end{cases}
$$

which can be simplified to $$S_{ij} = A_{ij} +S_{i-1,j} + S_{i,j-1} - S_{i-1,j-1}$$ if we define $S_{ij}$ to be $0$ when $(i,j)$ is out of bounds, or appropriately skip the out of bounds coordinates in our code.

This allows us to generate the table using the following code:

{{ code_block_of_file(path="max-submatrix.cpp", info_string="c++,linenos,hide_lines=1-20 37-85") }}

Using that table, we can calculate the sum of any contiguous submatrix in constant time. For example, the sum of all elements marked as `D` in

```txt
A A A B B B B B
A A A B B B B B
C C C D D D D D
C C C D D D D D
C C C D D D D D
```

is `D - B - C + A`, using the values of $S$ at the lower right corner of each region (we re-add `A` to compensate for the elements that are subtracted twice via `B` and `C`).

More formally,

$$\sum_{i=i_0,j=j_0}^{i_1,j_1} A_{ij} =
    S_{i_1,j_1} - S_{i_0-1,j_1} - S_{i_1,j_0-1} + S_{i_0-1,j_0-1}$$

if we define $S_{ij}$ to be $0$ when $(i,j)$ is out of bounds.


### Widest NzCSS

The lack of negative numbers in the matrix means that growing any contiguous submatrix will not result in a smaller sum of its elements. This means that for a given lower right corner at position $(i,j)$, instead of looking at all possible NzCSSs, we only need to consider the widest one.

Similarly to the previous section, we can take advantage of the calculations we've already done for previous coordinates to find the widest submatrix in constant time per coordinate. We generate a matrix $D$, such that $D_{ij}$ denotes the dimensions of the widest NzCSS with a lower right corner at $A_{ij}$. This obeys the following recurrence relation:


$$
D_{ij} =
    \begin{cases}
        0                                           & \text{if } A_{ij} = 0                  \\\\
        1                                           & \text{if } A_{ij} \neq 0 \land (i = 0 \lor j = 0) \\\\
        1 + \min(D_{i-1,j}, D_{i,j-1}, D_{i-1,j-1}) & \text{otherwise}
    \end{cases}
$$

In other words:

* if the element at the given coordinate is 0, the widest NzCSS is 0 too
* if we're on the left or top edge of the matrix (with a non-zero element), the widest NzCSS is 1
* otherwise the smallest of our west, north and northwest neighbor indicates how far the nearest zero or edge is, and we can increase that by one at our current position

The above relation allows us to generate the matrix using the following code:

{{code_block_of_file(path="max-submatrix.cpp", info_string="c++, linenos, hide_lines=1-37 50-85")}}


### Finding the result

Using $S$ and $D$, we can now find the answer by simply finding for each coordinate, the maximum sum NzCSS with it as a lower right corner, and keeping the largest among those:

{{code_block_of_file(path="max-submatrix.cpp", info_string="c++, linenos, hide_lines=1-53 79-85")}}


### Putting it all together

The [full source code](max-submatrix.cpp) is thus:

{{code_block_of_file(path="max-submatrix.cpp", info_string="c++, linenos")}}

Which outputs the correct result for the [example](#example):

```sh
$ c++ --std=c++20 max-submatrix.cpp && ./a.out < input.txt
Sum=1209 in 5-wide square starting at (3,4)
```

## Helper scripts

1. We use [`gen_input.py`](gen_input.py) to generate reproducible input data that fits the problem constraints:

{{code_block_of_file(path="gen_input.py", info_string="py, linenos")}}

2. We can test the correctness of the result for small inputs using a brute-force solution, for example [`maxSubmatrixBF.sc`](maxSubmatrixBF.sc), an Ammonite (Scala 3) script:

{{code_block_of_file(path="maxSubmatrixBF.sc", info_string="sc, linenos")}}

Using our input generator and a simple loop, we can decide what input sizes should be used to quickly test via a given brute-force solution:

```sh
$ for dim in 1 50 100 120 125; \
do
    echo - N=$dim; \
    ./gen_input.py $dim $dim 2038 6 \
        | tee >(`which time` -f '%E real' ./a.out >&2) \
        | `which time` -f '%E real' ./maxSubmatrixBF.sc;
done
- N=1
Sum=1326596463542876959 in 1-wide square starting at (0,0)
0:00.02 real
Sum=1326596463542876959 in 1-wide square starting at (0,0)
0:00.63 real
- N=50
Sum=289596670689973733 in 9-wide square starting at (13,12)
0:00.03 real
Sum=289596670689973733 in 9-wide square starting at (13,12)
0:00.84 real
- N=100
Sum=115092997091208499 in 11-wide square starting at (0,64)
0:00.58 real
Sum=115092997091208499 in 11-wide square starting at (0,64)
0:03.11 real
- N=120
Sum=66272117197447889 in 11-wide square starting at (54,87)
0:00.58 real
Sum=66272117197447889 in 11-wide square starting at (54,87)
0:06.33 real
- N=125
Sum=78999073335313433 in 12-wide square starting at (99,100)
0:00.60 real
Sum=78999073335313433 in 12-wide square starting at (99,100)
0:07.57 real
```

3. While debugging, we use [`cutRegion.sc`](cutRegion.sc) to narrow down the input that triggers a given issue:

{{code_block_of_file(path="cutRegion.sc", info_string="sc, linenos")}}