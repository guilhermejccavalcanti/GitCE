import java.util.ArrayList;

import util.FPFNCandidates;
import merger.FSTGenMerger;
import merger.MergeResult;



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
		LinkedList<MergeCommit> horizontalExecutionMergeCommits = fillMergeCommitsListForHorizontalExecution(projects)
		for(int i=0; i<horizontalExecutionMergeCommits.size();i++){
			MergeCommit m = horizontalExecutionMergeCommits.get(i);
			println ('Analysing ' + ((i+1)+'/'+horizontalExecutionMergeCommits.size()) + ': ' +  m.sha)

			Extractor ext = new Extractor(m)
			ext.downloadMergeScenario(m)
			if(m.revisionFile != null){
				FSTGenMerger merger 	  = new FSTGenMerger()
				
				MergeResult mergeResult	  = new MergeResult()
				mergeResult.projectName	  = m.projectName
				mergeResult.revision	  = m.revisionFile
								
				FPFNCandidates candidates = merger.runMerger(mergeResult)

				MethodReferencesFinderAST finder = new MethodReferencesFinderAST()
				finder.run(mergeResult,candidates.renamingCandidates, candidates.importCandidates, candidates.duplicatedCandidates)

				//TODO
				printMergeResult(mergeResult)
				
				fillExecutionLog(m.sha)
				String revisionFolderDir = (new File(m.revisionFile)).getParent()
				(new AntBuilder()).delete(dir:revisionFolderDir,failonerror:false)
			}
		}
	}

	def private static LinkedList<MergeCommit> fillMergeCommitsListForHorizontalExecution(ArrayList<Project> projects){
		ArrayList<String> alreadyExecutedSHAs = restoreExecutionLog();
		LinkedList<MergeCommit> horizontalExecutionMergeCommits = new LinkedList<MergeCommit>()
		int aux = projects.size()
		int i 	= 0;
		while(i < projects.size()) {
			Project p = projects.get(i)
			if(!p.listMergeCommit.isEmpty()){
				MergeCommit mergeCommit = p.listMergeCommit.poll()
				if(!alreadyExecutedSHAs.contains(mergeCommit.sha)){
					horizontalExecutionMergeCommits.add(mergeCommit)
				}
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

	def private static fillExecutionLog(String lastMergeCommitSHA){
		def out = new File('execution.log')
		out.append lastMergeCommitSHA
		out.append '\n'
	}

	def private static ArrayList<String> restoreExecutionLog(){
		ArrayList<String> alreadyExecutedSHAs = new ArrayList<String>()
		try {
			BufferedReader br = new BufferedReader(new FileReader("execution.log"))
			String line  = ""
			while ((line = br.readLine()) != null)
				alreadyExecutedSHAs.add(line)
		} catch (FileNotFoundException e) {}
		return alreadyExecutedSHAs
	}
	
	def private static printMergeResult(MergeResult mergeResult){
		
	}

	public static void main (String[] args){
		ArrayList<Project> projects = readProjects();
		runFPFNAnalysis(projects)
	}
}
