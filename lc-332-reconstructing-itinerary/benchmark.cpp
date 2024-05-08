#include <benchmark/benchmark.h>
#include <format>
#include <fstream>
#include <iostream>
#include <queue>
#include <set>
#include <stack>
#include <unordered_map>

using std::string;
using std::unordered_map;
using std::vector;

static void AllInputs(benchmark::internal::Benchmark* b) {
    b
        ->Args({2, 2})

        ->Args({2, 300})
        ->Args({17, 300})
        ->Args({150, 300})
        ->Args({300, 300})

        ->Args({2, 17576})
        ->Args({132, 17576})
        ->Args({8788, 17576})
        ->Args({17576, 17576})
        ;
}

vector<vector<string>> readInput(string fileName) {
    std::ifstream in(fileName);
    if (!in) {
        std::cerr << "Error opening " << fileName << std::endl;
        std::exit(1);
    }

    int M; // number of edges
    in >> M;

    vector<vector<string>> tickets;

    for (int i=0; i<M; i++) {
        std::vector<string> ticket(2);

        in >> ticket[0] >> ticket[1];

        tickets.push_back(ticket);
    }

    return tickets;
}

template <class T> void solve(benchmark::State& state) {
    auto nodeCnt = state.range(0);
    auto edgeCnt = state.range(1);

    const auto tickets = readInput(std::format("in/{}.{}", nodeCnt, edgeCnt));

    T solver;

    for (auto _ : state) {
        state.PauseTiming();
        auto ticketsCopy = tickets;
        state.ResumeTiming();
        auto result = solver.findItinerary(ticketsCopy);
    }
}

// XXX: needs to be more DRY below

class GlobalSort {
public:
    vector<string> findItinerary(vector<vector<string>>& tickets) {
        using std::stack;

        std::sort(tickets.begin(), tickets.end(), [](auto const& lhs, auto const& rhs) {
            return lhs > rhs;
        });

        unordered_map<string, stack<string>> adj;
        for (const auto& t: tickets)
            adj[t[0]].push(t[1]);

        vector<string> post_order_acc;
        dfs("JFK", adj, post_order_acc);

        std::reverse(post_order_acc.begin(), post_order_acc.end());
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
BENCHMARK(solve<GlobalSort>)->UseRealTime()->Apply(AllInputs);

class MinPriQ {
public:
    vector<string> findItinerary(vector<vector<string>>& tickets) {
        using std::greater;
        using std::priority_queue;

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
BENCHMARK(solve<MinPriQ>)->UseRealTime()->Apply(AllInputs);

class Make_heap {
public:
    vector<string> findItinerary(vector<vector<string>>& tickets) {
        unordered_map<string, vector<string>> adj;
        for (const auto& t: tickets)
            adj[t[0]].push_back(t[1]);

        for (auto& [_, neighbors]: adj)
            // turn into a min heap
            std::make_heap(neighbors.begin(), neighbors.end(), std::greater<>{});

        vector<string> post_order_acc;
        dfs("JFK", adj, post_order_acc);

        reverse(post_order_acc.begin(), post_order_acc.end());
        return post_order_acc;
    }

private:
    void dfs(const auto& v, auto& adj, auto& post_order_acc) {
        if (adj.contains(v))
            while (!adj[v].empty()) {
                std::pop_heap(adj[v].begin(), adj[v].end(), std::greater<>{});
                const string u = adj[v].back();
                adj[v].pop_back();
                dfs(u, adj, post_order_acc);
            }
        post_order_acc.push_back(v);
    }
};
BENCHMARK(solve<Make_heap>)->UseRealTime()->Apply(AllInputs);

class LocalSort {
public:
    vector<string> findItinerary(vector<vector<string>>& tickets) {
        unordered_map<
            string,
            vector<string>> adj;
        for (const auto& t: tickets)
            adj[t[0]].push_back(t[1]);

        for (auto& [node, neighbors]: adj)
            std::sort(neighbors.begin(), neighbors.end(), [](auto const& lhs, auto const& rhs) {
                return lhs > rhs;
            });

        vector<string> post_order_acc;
        dfs("JFK", adj, post_order_acc);

        reverse(post_order_acc.begin(), post_order_acc.end());
        return post_order_acc;
    }

private:
    void dfs(const auto& v, auto& adj, auto& post_order_acc) {
        if (adj.contains(v))
            while (!adj[v].empty()) {
                const string u = adj[v].back();
                adj[v].pop_back();
                dfs(u, adj, post_order_acc);
            }
        post_order_acc.push_back(v);
    }
};
BENCHMARK(solve<LocalSort>)->UseRealTime()->Apply(AllInputs);

class Multiset {
public:
    vector<string> findItinerary(vector<vector<string>>& tickets) {
        using std::multiset;

        unordered_map<string, multiset<string>> adj;
        for (const auto& t: tickets)
            adj[t[0]].insert(t[1]);

        vector<string> post_order_acc;
        dfs("JFK", adj, post_order_acc);

        reverse(post_order_acc.begin(), post_order_acc.end());
        return post_order_acc;
    }

private:
    void dfs(const auto& v, auto& adj, auto& post_order_acc) {
        if (adj.contains(v))
            while (!adj[v].empty()) {
                const string u = *adj[v].begin();
                adj[v].erase(adj[v].begin());
                dfs(u, adj, post_order_acc);
            }
        post_order_acc.push_back(v);
    }
};
BENCHMARK(solve<Multiset>)->UseRealTime()->Apply(AllInputs);

BENCHMARK_MAIN();
