#include <algorithm>
#include <fstream>
#include <iostream>
#include <queue>
#include <unordered_map>
#include <vector>

using std::greater;
using std::priority_queue;
using std::reverse;
using std::string;
using std::unordered_map;
using std::vector;

class Solution {
	public:
		vector<string> findItinerary(vector<vector<string>>& tickets) {
			unordered_map<
				string,
				vector<string>> adj;
			for (const auto& t: tickets)
				adj[t[0]].push_back(t[1]);

			for (auto& [_, neighbors]: adj)
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

void readInput(string fileName, auto& tickets) {
	std::ifstream in(fileName);
	if (!in) {
		std::cerr << "Error opening " << fileName << std::endl;
		std::exit(1);
	}

	int M; // number of edges
	in >> M;

	for (int i=0; i<M; i++) {
		std::vector<string> ticket(2);

		in >> ticket[0] >> ticket[1];

		tickets.push_back(ticket);
	}
}

int main(int argc, char* argv[]) {
	if (!(argc == 2
				|| (argc == 3 && std::string(argv[2]) == "--only-read"))) {
		std::cerr << "usage: " << argv[0] << " <FILE> [--only-read]\n";
		return 1;
	}

	vector<vector<string>> tickets;
	readInput(argv[1], tickets);

	if (argc == 3 && std::string(argv[2]) == "--only-read")
		return 0;

	Solution s;
	for (auto&& airport: s.findItinerary(tickets))
		std::cout << airport << std::endl;
}
