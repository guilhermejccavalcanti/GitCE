import java.util.ArrayList;

import util.FPFNCandidates;
import merger.FSTGenMerger;


class App {

	def static run(){
		Read r = new Read("projects.csv")
		def projects = r.getProjects()
		println('Reader Finished!')

		projects.each {
			GremlinQuery gq = new GremlinQuery(it.graph)

			Printer p = new Printer()
			p.writeCSV(gq.getMergeCommitsList())
			println('Printer Finished!')
			println("----------------------")

			it.setMergeCommits(gq.getMergeCommitsList())

			Extractor e = new Extractor(it)
			e.extractCommits(Strategy.ALL)
			println('Extractor Finished!\n')
			gq.graph.shutdown();
		}
	}

	def static runWithCommitCsv(){
		// running directly with the commit list in a CSV file

		Read r = new Read("projects.csv")
		def projects = r.getProjects()
		println('Reader Finished!')

		projects.each {
			r.setCsvCommitsFile("commits.csv")
			r.readCommitsCSV()
			def ls = r.getMergeCommitsList()

			it.setMergeCommits(ls)

			Extractor e = new Extractor(it)
			e.extractCommits(Strategy.ALL)
			println('Extractor Finished!\n')
		}
	}

	def static collectMergeCommits(){
		Read r = new Read("projects.csv")
		def projects = r.getProjects()
		println('Reader Finished!')

		projects.each {
			GremlinQuery gq = new GremlinQuery(it.graph)

			Printer p = new Printer()
			p.writeCSV(gq.getMergeCommitsList())
			println('Printer Finished!')
			println("----------------------")

			it.setMergeCommits(gq.getMergeCommitsList())
			gq.graph.shutdown();
		}
	}

	def static ArrayList<Project> readProjects(){
		Read r = new Read("projects.csv")
		def projects = r.getProjects()

		projects.each {
			GremlinQuery gq = new GremlinQuery(it.graph)

			Printer p = new Printer()
			p.writeCSV(gq.getMergeCommitsList())

			it.setMergeCommits(gq.getMergeCommitsList())

			Extractor e = new Extractor(it)
			e.fillAncestors()
			println('Project ' + it.name + " read")
			gq.graph.shutdown();
		}

		return projects
	}

	def static runFPFNAnalysis(ArrayList<Project> projects){
		LinkedList<MergeCommit> horizontalExecutionMergeCommits = this.fillMergeCommitsListForHorizontalExecution(projects);
		println 'Horizontal List'
		for(MergeCommit m : horizontalExecutionMergeCommits){
			println m.toString()
		}
		
		for(MergeCommit m : horizontalExecutionMergeCommits){
			Extractor ext = new Extractor(m)
			ext.downloadMergeScenario(m)
			if(m.revisionFile != null){
				FSTGenMerger merger 	  = new FSTGenMerger();
				FPFNCandidates candidates = merger.run(m.revisionFile);
				//executa análise de ast em candidates
			}
		}
	}

	def private static LinkedList<MergeCommit> fillMergeCommitsListForHorizontalExecution(ArrayList<Project> projects){
		LinkedList<MergeCommit> horizontalExecutionMergeCommits = new LinkedList<MergeCommit>()
		int aux = projects.size()
		int i 	= 0;
		while(i < projects.size()) {
			Project p = projects.get(i)
			if(!p.listMergeCommit.isEmpty()){
				MergeCommit mergeCommit = p.listMergeCommit.poll()
				horizontalExecutionMergeCommits.add(mergeCommit)
			}
			if(p.listMergeCommit.isEmpty()){
				projects.remove(i)
			}
			aux 	= projects.size()
			if(aux == 0){
				break
			}
			if(i >= (projects.size() - 1)){
				i = 0;
			} else {
				i++;
			}
		}
		return horizontalExecutionMergeCommits
	}

	public static void main (String[] args){
		ArrayList<Project> projects = readProjects();
		runFPFNAnalysis(projects)
	}
}
