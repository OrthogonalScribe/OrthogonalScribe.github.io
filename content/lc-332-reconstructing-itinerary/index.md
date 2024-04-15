+++
title = "LeetCode 332: Reconstructing Itinerary"
description = "Using Hierholzer's algorithm to find an Eulerian trail and optimizing its performance"
date = 2024-03-27
updated = 2024-04-15

[taxonomies]
tags = ["Algorithms", "Benchmarking", "Optimization", "Google Benchmark", "LeetCode", "Graphs", "Backtracking", "C++", "Python"]

[extra]
math = true
keywords = "Algorithms, Graphs, Hierholzer, Benchmarking, Optimization, Google Benchmark, Backtracking, C++, Python, LeetCode, Reconstructing Itinerary"
toc = true
series = "Algorithms"
+++

This post explains how [an implementation of Hierholzer's algorithm](https://walkccc.me/LeetCode/problems/332/) solves [LeetCode 332: Reconstructing Itinerary](https://leetcode.com/problems/reconstruct-itinerary/) to find an Eulerian trail, and benchmarks the performance of several data structures and algorithms that can be used.


<!-- more -->


## Problem statement

We are given a set of tickets between airports, represented as a list of 2-element lists, such as `[["MUC","LHR"],["JFK","MUC"],["SFO","SJC"],["LHR","SFO"]]`. We are to find an itinerary that uses those tickets, such that:
* all tickets are used exactly once
* the itinerary must be the lexicographically smallest one - for example "`["JFK", "LGA"]` has a smaller lexical order than `["JFK", "LGB"]`"

Constraints:
* there are no more than 300 tickets
* the input forms at least one valid itinerary

The two examples can be seen in [the problem description on LeetCode](https://leetcode.com/problems/reconstruct-itinerary/description/).


### Translation

We are given the edges of an at least semi-Eulerian directed graph, and are to find the lexicographically minimal Eulerian trail in it.


## Solution

We'll be starting with a modified implementation of [Hierholzer's algorithm](https://en.wikipedia.org/wiki/Eulerian_path#Hierholzer's_algorithm) from [walkccc.me](https://walkccc.me/LeetCode/problems/332/#__tabbed_1_3):

```py3, linenos
class Solution:
  def findItinerary(self, edges: List[List[str]]) -> List[str]:
    # adj[i] is a stack of all of i's successors; popping from
    # it gives us the next lexicographically minimal one
    adj = collections.defaultdict(list)

    # reverse-sort the edges, so that the adjacency stacks can
    # be popped in an ascending lexicographical order
    for v, u in reversed(sorted(edges)):
      adj[v].append(u)

    post_order_acc = []

    def dfs(v: str) -> None:
      # process all outgoing edges
      while v in adj and adj[v]:
        dfs(adj[v].pop())
      post_order_acc.append(v)

    dfs('JFK')
    return reversed(post_order_acc)
```


### Explanation

Hierholzer's algorithm can be summarized as:

1. start from any vertex and follow a trail of edges—removing them once they've been used—until we get stuck at a vertex with no more outgoing edges
1. start from any other vertex on the found trail that has outgoing edges remaining, and repeat the process
1. repeat this until all edges have been used
1. merge the resulting trails appropriately

Compared to the summary, the implementation shown above uses a recursive depth-first traversal to follow the trails. Using recursion gives us implicit backtracking, and we can begin traversal from the `JFK` vertex. We generate the adjacency lists such that we always follow the lexicographically minimal successor of a given vertex first, to ensure a lexicographically minimal Eulerian trail. We also use a reverse post-order accumulation of the result to handle the merging of trails in the appropriate order.

The key intuition for reverse post-order traversal merging all the trails appropriately and producing the right Eulerian trail is that for each vertex that is a fork (any vertex with more than one outgoing edge):

* each outgoing edge will lead to either a circuit (a closed trail coming back to the fork), or at most one open trail finishing at either the only vertex with indegree=outdegree+1 (if there is one), or at `JFK`
* the one potential outgoing open trail may be traversed at any time among the other outgoing circuits, but it will be the first branch to result in backtracking. This results in it being added first to the accumulator, and thus printed last, as if we had traversed the open trail "last" in the first place
* all outgoing circuits before and after the outgoing open trail will loop through the fork vertex, finally exhausting it and starting to backtrack from it. This goes back up the lexicographically biggest circuit, then the one smaller and so on, resulting in the circuits being spliced in the correct order

The resulting time and space complexities are $\mathcal{O}(|E| log |E|)$ and $\mathcal{O}(|E|)$, respectively.


### C++ implementation

Implementing the algorithm in C++, we use a min priority queue to do the sorting for us, ending up with:

```c++, linenos
class Solution {
public:
    vector<string> findItinerary(vector<vector<string>>& tickets) {
        unordered_map<
            string,
            priority_queue<string, vector<string>, greater<string>>> adj;
        for (const auto& t: tickets)
            adj[t[0]].push(t[1]);

        vector<string> post_order_acc;
        dfs("JFK", adj, post_order_acc);

        reverse(post_order_acc.begin(), post_order_acc.end());
        return post_order_acc;
    }

private:
    void dfs(const auto& v, auto& adj, auto& post_order_acc) {
        if (adj.contains(v))
            while (!adj[v].empty()) {
                const string u = adj[v].top();
                adj[v].pop();
                dfs(u, adj, post_order_acc);
            }
        post_order_acc.push_back(v);
    }
};
```


## Performance

While the above solutions are sufficiently fast to serve as LeetCode submissions, spending a bit of time thinking about performance can be useful for future problems that have some overlap with this one.

The loglinear time complexity stems from us needing to find the lexicographically minimal Eulerian trail. As an aside, in scenarios where any Eulerian trail is acceptable, we can skip the edge or neighbor ordering and achieve $\mathcal{O}(|V| + |E|)$[^tum_complexity].

Looking a bit closer, the precise requirement is to always choose the next lexicographically smallest neighbor to traverse when processing a particular vertex. This gives us the first performance knob we can tweak. Our Python solution achieves that ordered processing by globally sorting all edges in descending order and using lists as stacks, but this is not strictly necessary—we only need per-vertex ordering. Thus, in our C++ solution we use a min priority queue (the same can be done in Python using [heapq](https://docs.python.org/3/library/heapq.html)). While this is still loglinear, the log factor is now the largest outdegree of a vertex in the graph, which can be a notable difference in the general case. Within the constraints of the LeetCode problem, $|E| < 300$ means that the best case scenario (maximum outdegree of 1) might enjoy up to an 8x speedup (of that part of the code specifically) compared to the worst case scenario.

However, this is ignoring constant factors in both the ordering steps and the rest of the algorithm, as well as data locality and other practical concerns. As usual, it is best to measure to ensure the theoretical improvements translate into real speedups.

LeetCode's measurement infrastructure can often produce notable runtime variance, especially with shorter execution times - for example with this problem (average runtime of 17 ms), we can see 5th percentile to 80th percentile jumps when resubmitting the same code a minute later. This is usually sufficient for knowing whether one's solution has the desired time complexity, but not ideal if we want to test for speedups on the order of 2x or less. In order to do so, we'll use the [Google Benchmark](https://github.com/google/benchmark) library and our own inputs to test the performance of several approaches.

### Inputs and approaches tested

We test the following approaches:

* an equivalent of the global sort Python solution,
* the minimum priority queue C++ solution,
* a `std::make_heap` version of the above to achieve linear adjacency list initialization per vertex,
* the C++ multiset[^multiset_codeforces] solution from [walkccc.me](https://walkccc.me/LeetCode/problems/332/#__tabbed_1_1),
* and manually sorting the edges in each adjacency list.

We test those with a mix of randomly generated Eulerian and semi-Eulerian graphs. Those are generated in a reproducible fashion using a simple algorithm that augments a [cycle graph](https://en.wikipedia.org/wiki/Cycle_graph) with random two-vertex round-trips. We generate graphs in three categories based on their edge count:

* minimal (2 edges),
* maximum within the LeetCode constraints (300 edges),
* and maximum within the airport identifier constraints ($26^3$, 17576).

The largest vertex outdegree in these graphs can be expected to decrease with the increase of the node to edge ratio, and we expect performance for the local ordering approaches to scale with the logarithm of that, so in each edge category we generate graphs

* with the minimum node count (2 nodes),
* at the logarithmic halfway point ($\sqrt{|E|}$ nodes),
* at the halfway point ($|V| = \frac{|E|}{2}$),
* and with the minimum outdegree count of 1 ($|V| = |E|$).


### Results

* As expected, we observe a speedup with bigger graphs with the local ordering approaches, both within the LeetCode constraints (up to 1.6x), and up to 2.4x with $26^3$-edge graphs. However, some of the local ordering approaches are slower than the global sorting one with smaller graphs.
* We observe a 5-10x speedup across the board when switching from compiling with `-O0` to `-O2`.
* Explicit local sorting is always faster than global sorting, trading blows for first place with the multiset approach depending on whether we use `-O0` or `-O2`.
* The priority queue solutions are the middle of the pack, roughly on par with global sorting at $(|E| = 300, |V| = 17)$ and faster with larger inputs. The used `std::make_heap` approach is slightly slower than the `std::priority_queue` one.
* The multiset solution is the fastest when using `-O0`, but between the priority queue solutions when using `-O2` and even slower than them on $26^3$-edge graphs.
* All solutions take a dip in absolute performance at the $|V| = \sqrt{|E|}$ test point. This is for both the 300- and $26^3$-edge scenarios, so it is less likely to be due to a particularly bad input graph. Further investigation is needed to find out the cause.

Notably, unless we want to do a manual local sort, the semantically most direct translation (min priority queue) is the next fastest approach when using `-O2`.


#### With `-O0`

Comparative speedups:

{{include_file(path="log/o0.mdtable")}}

<details>
    <summary>Invocation used to generate the table</summary>

`make clean benchmark && sudo cpupower frequency-set -f 3000000 && ./benchmark --benchmark_min_warmup_time=1 --benchmark_repetitions=3 | tee log/o0.txt | grep real_time_median | ./bench2table.sc --base GlobalSort --column Make_heap --column MinPriQ --column LocalSort --column Multiset > log/o0.mdtable ; sudo cpupower frequency-set --governor ondemand`

</details>

<details>
    <summary>Benchmark log</summary>

{{code_block_of_file(path="log/o0.txt", info_string="plain")}}

</details>

#### With `-O2`

Comparative speedups:

{{include_file(path="log/o2.mdtable")}}

<details>
    <summary>Invocation used to generate the table</summary>

`make clean benchmark OPTFLAGS=-O2 && sudo cpupower frequency-set -f 3000000 && ./benchmark --benchmark_min_warmup_time=1 --benchmark_repetitions=3 | tee log/o2.txt | grep real_time_median | ./bench2table.sc --base GlobalSort --column Make_heap --column Multiset --column MinPriQ --column LocalSort > log/o2.mdtable ; sudo cpupower frequency-set --governor ondemand`

</details>

<details>
    <summary>Benchmark log</summary>

{{code_block_of_file(path="log/o2.txt", info_string="plain")}}

</details>

### Methods


We generate the graphs via [genEulerianGraph.sc](./genEulerianGraph.sc):

<details>
    <summary>Click to expand</summary>

{{code_block_of_file(path="genEulerianGraph.sc", info_string="sc, linenos")}}

</details>

The benchmark source code is [benchmark.cpp](./benchmark.cpp):

<details>
    <summary>Click to expand</summary>

{{code_block_of_file(path="benchmark.cpp", info_string="c++, linenos")}}

</details>

`PauseTiming()` and `ResumeTiming()` were measured to have a combined overhead of approximately 350 ns in both optimization scenarios.

We compile the benchmark with `gcc (Debian 13.2.0-10) 13.2.0` and `-std=c++20`, in two configurations: with `-O2`, and without any `-O` flags.[^zola_extensionless]

We use `libbenchmark-dev:amd64` version `1.8.3-3` on a Debian Testing machine.

Guided by the [documentation](https://google.github.io/benchmark/reducing_variance.html), we disable CPU scaling to reduce result variance. In this particular case, the CPU exhibited significant frequency swings when using the `performance` governor, so its frequency was manually locked via

`sudo cpupower frequency-set -f 3000000`[^benchmark_scaling_reporting]

The current CPU frequency can be monitored with

`watch -n1 cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq`

and the current governor can be seen via

`head /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor`[^arch_wiki_cpu_scaling]


Finally, we massage the console output[^google_json] via [bench2table.sc](./bench2table.sc):

<details>
    <summary>Click to expand</summary>

{{code_block_of_file(path="bench2table.sc", info_string="sc, linenos")}}

</details>


### Limitations

The input data for the benchmarks is somewhat limited—we only test against a single graph per $(|E|, |V|)$ combination, and our generator is most likely to produce graphs with a relatively flat vertex outdegree distribution, which may unfairly benefit the local ordering approaches.

Benchmark execution might be affected by hyper-threading not being disabled, and the benchmark executable is linked against the OS-provided version of the library, which is a debug build.

## Footnotes

[^tum_complexity]: [TUM: Hierholzer's algorithm](https://algorithms.discrete.ma.tum.de/graph-algorithms/hierholzer/index_en.html#tab_tw) / More / How fast is the algorithm?

[^multiset_codeforces]: See [this Codeforces blog post](https://codeforces.com/blog/entry/69230) for further discussion of some possible trade-offs between using `std::multiset` and `std::priority_queue` for priority queues.

[^zola_extensionless]: The intention is to include the used `Makefile` once a related [Zola issue](https://github.com/getzola/zola/issues/2354) is sufficiently resolved.

[^benchmark_scaling_reporting]: Google Benchmark still reports active CPU scaling in this case, as well as when using the `powersave` governor. This is understandable in the manual frequency setting case, as that is reported as the `userspace` governor, which allows any `EUID=0` process to adjust the CPU frequency at any point in time.

[^arch_wiki_cpu_scaling]: For further information, see [Arch wiki: CPU frequency scaling](https://wiki.archlinux.org/title/CPU_frequency_scaling).

[^google_json]: Google Benchmark also [supports CSV (deprecated) and JSON output formatting](https://github.com/google/benchmark/blob/main/docs/user_guide.md#output-formats).