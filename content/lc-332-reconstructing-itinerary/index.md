+++
title = "LeetCode 332: Reconstructing Itinerary"
description = "Using Hierholzer's algorithm to find an Eulerian trail"
date = 2023-03-27

[taxonomies]
tags = ["Algorithms", "Competitive Programming", "LeetCode", "Graphs", "Backtracking", "C++", "Python"]

[extra]
math = true
#math_auto_render = true
keywords = "Algorithms, Competitive programming, Graphs, Hierholzer, Backtracking, C++, Python, LeetCode, Reconstructing Itinerary"
toc = true
series = "Algorithms"
+++

This post explains how [an implementation of Hierholzer's algorithm](https://walkccc.me/LeetCode/problems/332/) solves [LeetCode 332: Reconstructing Itinerary](https://leetcode.com/problems/reconstruct-itinerary/) to find an Eulerian trail.


<!-- more -->


## Problem statement

We are given a set of tickets between airports, represented as a list of 2-element lists, such as `[["MUC","LHR"],["JFK","MUC"],["SFO","SJC"],["LHR","SFO"]]`. We are to find an itinerary that uses those tickets, such that:
* all tickets are used exactly once
* the itinerary must be the lexicographically smallest one - for example "`["JFK", "LGA"]` has a smaller lexical order than `["JFK", "LGB"]`"

Constraints:
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

(we can also use [min priority queues](https://docs.python.org/3/library/heapq.html) in place of explicit sorting and stacks)

### Explanation

Hierholzer's algorithm can be summarized as:
1. start from any vertex and follow a trail of edges—removing them once they've been used—until we get stuck at a vertex with no more outbound edges
1. start from any other vertex on the found trail that has outbound edges remaining, and repeat the process
1. repeat this until all edges have been used
1. merge the resulting trails appropriately

Compared to the summary, the implementation shown above uses a recursive depth-first traversal to follow the trails. Using recursion gives us backtracking for free, and we can begin traversal from the `JFK` vertex. We generate the adjacency lists such that we always follow the lexicographically minimal successor of a given vertex first to ensure a lexicographically minimal Eulerian trail. We also use a reverse post-order accumulation of the result to handle the merging of trails in the appropriate order.

The key intuition for reverse post-order traversal merging all the trails appropriately and producing the right Eulerian trail is that for each vertex that is a fork (any vertex with outdegree more than 1):
* each outgoing edge will lead to either a circuit (a closed trail coming back to the fork), or at most one open trail finishing at either the only vertex with indegree=outdegree+1 (if there is one), or at `JFK`
* the one potential outgoing open trail may be traversed at any time among the other outgoing circuits, but it will be the first branch to result in backtracking. This results in it being added first to the accumulator, and thus printed last, as if we had traversed the open trail "last" in the first place
* all outgoing circuits before and after the outgoing open trail will loop through the fork vertex, finally exhausting it and starting to backtrack from it. This goes back up the lexicographically biggest circuit, then the one smaller and so on, resulting in the circuits being spliced in the correct order

The resulting time and space complexities are $\mathcal{O}(|E| log |E|)$ and $\mathcal{O}(|E|)$, respectively.


### C++ translation

Translating the above to C++, we use a min priority queue to do the sorting for us, ending up with:

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

 We can also use a multiset in place of a min priority queue, which [can involve some performance trade-offs](https://codeforces.com/blog/entry/69230).