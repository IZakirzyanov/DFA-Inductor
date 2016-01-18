package EDSM;

import algorithms.StateMerger;
import structures.APTA;
import structures.Node;

import java.util.*;

public class EDSMWorker {

	public enum EDSMHeuristic{
		Status, Fanout
	}

	private APTA apta;
	private StateMerger merger;

	private final boolean randomMode;
	private final int positiveSizeBound;

	public EDSMWorker(APTA apta, EDSMHeuristic heuristic, boolean randomMode,
	                  int positiveSizeBound, int pathsLowerBound, int pathsOnSymbolLowerBound) {
		this.apta = apta;
		switch (heuristic) {
			case Status:
				merger = new StatusEDSMMerger(apta);
				break;
			case Fanout:
				merger = new FanoutEDSMMerger(apta, pathsLowerBound, pathsOnSymbolLowerBound);
				break;
		}
		redNodes = new HashSet<>();
		blueNodes = new HashSet<>();
		initRedBlue();

		this.randomMode = randomMode;
		this.positiveSizeBound = positiveSizeBound;
	}

	public boolean startMerging() {
		//TODO: out condition
		while (true) {
			if (apta.getSize() < 200) {
				break;
			}
			MergePair mergePair = findBestMerge();
			if (mergePair.score < 0) {
				updateRedBlue(mergePair.blue);
			} else {
				merge(mergePair);
			}
		}
		// wrong!
		return true;
	}

	private MergePair findBestMerge() {
		MergePair pair;
		MergePair bestPair = new MergePair();
		double score;
		for (Node blue : blueNodes) {
			pair = new MergePair();
			for (Node red : redNodes) {
				score = mergeAndUndo(red, blue);
				if (randomMode && score > 0) {
					score *= Math.random();
				}
				if (score > pair.score || pair.score == 0) {
					pair.update(red, blue, score);
				}
			}
			if (pair.score > 0) {
				if (pair.score > bestPair.score) {
					bestPair = pair;
				}
			}
			if (pair.score < 0) {
				if (bestPair.score < 0 && bestPair.score < pair.score || bestPair.score == 0) {
					bestPair = pair;
				}
			}
		}
		return bestPair;
	}

	private int merge(MergePair pair) {
		Node red = pair.red;
		Node blue = pair.blue;
		merger.resetScore();
		merger.merge(red, blue, true);
		return merger.getScore();
	}

	private int mergeAndUndo(Node red, Node blue) {
		merger.resetScore();
		merger.merge(red, blue, false);
		int res = merger.getScore() >= 0 ? merger.getScore() : -blue.getDepth();
		merger.undoMerge(red, blue);
		return res;
	}

	private void initRedBlue() {
		redNodes.add(apta.getRoot());
		for (Node redNode : redNodes) {
			for (Node candidateBlueNode : redNode.getChildren().values()) {
				if (!redNodes.contains(candidateBlueNode)) {
					blueNodes.add(candidateBlueNode);
				}
			}
		}
	}

	private void updateRedBlue(Node blue) {
		blueNodes.remove(blue);
		redNodes.add(blue);
		for (Node newBlue : blue.getChildren().values()) {
			blueNodes.add(newBlue);
		}
	}

	class MergePair {
		Node red;
		Node blue;
		double score;

		MergePair() {
			score = 0;
		}

		MergePair(Node red, Node blue, double score) {
			this.red = red;
			this.blue = blue;
			this.score = score;
		}

		MergePair(Node red, Node blue, boolean consistent) {
			this.red = red;
			this.blue = blue;
			score = Integer.MIN_VALUE;
		}

		void update(Node red, Node blue, double score) {
			this.red = red;
			this.blue = blue;
			this.score = score;
		}
	}
}
