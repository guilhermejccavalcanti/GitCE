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
		restoreGitRepositories(projects)
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

				printMergeResult(mergeResult)

				fillExecutionLog(m)
				String revisionFolderDir = (new File(m.revisionFile)).getParent()
				(new AntBuilder()).delete(dir:revisionFolderDir,failonerror:false)
			}
		}
	}
	
	def private static restoreGitRepositories(ArrayList<Project> projects){
		//Read r = new Read("projects.csv")
		//def projects = r.getProjects()
		projects.each {
			Extractor e = new Extractor(it)
			e.restoreWorkingFolder()
		}
		println('Restore finished!\n')
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
				if(!alreadyExecutedSHAs.contains(mergeCommit.projectName+','+mergeCommit.sha)){
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

	def private static fillExecutionLog(MergeCommit lastMergeCommit){
		def out = new File('execution.log')
		out.append (lastMergeCommit.projectName+','+lastMergeCommit.sha)
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
		//first, load previous stored results if it exists and update it
		def rows = new ArrayList<String>()
		boolean projectFound = false
		def project
		def mergeScenarios
		def fpOrderingMergeScenarios
		def fpRenamingMergeScenarios
		def fnDuplicationMergeScenarios
		def fnImportMergeScenarios
		def textualConfUnmerge
		def textualConfSsmerge
		def fpOrderingConf
		def fpRenamingConf
		def fnDuplicationMissed
		def fnImportMissed

		//mergeResult.orderingConflicts = mergeResult.linedbasedConfs - (mergeResult.ssmergeConfs - mergeResult.renamingConflictsFromSsmerge)
		
		int tssmerge = (mergeResult.ssmergeConfs+mergeResult.importIssuesFromParser+mergeResult.importIssuesFromSsmergePackageMember);
		mergeResult.orderingConflicts = ((mergeResult.linedbasedConfs - (tssmerge - mergeResult.renamingConflictsFromSsmerge))>0)?(mergeResult.linedbasedConfs - (tssmerge - mergeResult.renamingConflictsFromSsmerge)):0
		
		new File('results/resultFPFNAnalysis.csv').splitEachLine(' ') {fields ->
			project = fields[0]
			if(project == mergeResult.projectName){
				mergeScenarios 					= fields[1].toInteger() + 1
				fpOrderingMergeScenarios 		= (mergeResult.orderingConflicts>0)?((fields[2]).toInteger()+1):(fields[2])
				fpRenamingMergeScenarios 		= ((mergeResult.renamingConflictsFromSsmerge-mergeResult.renamingConflictsFromParser)>0)?((fields[3]).toInteger()+1):(fields[3])
				fnDuplicationMergeScenarios 	= (mergeResult.duplicationIssuesFromParser>0)?((fields[4]).toInteger()+1):(fields[4])
				fnImportMergeScenarios 			= ((mergeResult.importIssuesFromParser + mergeResult.importIssuesFromSsmergePackageMember)>0)?((fields[5]).toInteger()+1):(fields[5])
				textualConfUnmerge				= fields[6].toInteger()+mergeResult.linedbasedConfs
				textualConfSsmerge				= fields[7].toInteger()+mergeResult.ssmergeConfs
				fpOrderingConf 					= fields[8].toInteger()+mergeResult.orderingConflicts
				fpRenamingConf 					= fields[9].toInteger()+(mergeResult.renamingConflictsFromSsmerge-mergeResult.renamingConflictsFromParser)
				fnDuplicationMissed 			= fields[10].toInteger()+mergeResult.duplicationIssuesFromParser
				fnImportMissed 					= fields[11].toInteger()+(mergeResult.importIssuesFromParser + mergeResult.importIssuesFromSsmergePackageMember)
				projectFound = true;
				def updatedRow = [project,mergeScenarios,
					fpOrderingMergeScenarios,fpRenamingMergeScenarios,
					fnDuplicationMergeScenarios,fnImportMergeScenarios,
					textualConfUnmerge,textualConfSsmerge,
					fpOrderingConf,fpRenamingConf,
					fnDuplicationMissed,fnImportMissed]
				rows.add(updatedRow.join(' '))
			} else {
				rows.add(fields.join(' '))
			}
		}
		//otherwise, create a new instance
		if(!projectFound){
			project 						= mergeResult.projectName
			mergeScenarios 					= 1
			fpOrderingMergeScenarios 		= (mergeResult.orderingConflicts>0)?1:0
			fpRenamingMergeScenarios 		= ((mergeResult.renamingConflictsFromSsmerge-mergeResult.renamingConflictsFromParser)>0)?1:0
			fnDuplicationMergeScenarios 	= (mergeResult.duplicationIssuesFromParser>0)?1:0
			fnImportMergeScenarios 			= ((mergeResult.importIssuesFromParser + mergeResult.importIssuesFromSsmergePackageMember)>0)?1:0
			textualConfSsmerge				= mergeResult.ssmergeConfs
			textualConfUnmerge				= mergeResult.linedbasedConfs
			fpOrderingConf 					= mergeResult.orderingConflicts
			fpRenamingConf 					= (mergeResult.renamingConflictsFromSsmerge-mergeResult.renamingConflictsFromParser)
			fnDuplicationMissed 			= mergeResult.duplicationIssuesFromParser
			fnImportMissed 					= mergeResult.importIssuesFromParser + mergeResult.importIssuesFromSsmergePackageMember

			def newRow = [project,mergeScenarios,
				fpOrderingMergeScenarios,fpRenamingMergeScenarios,
				fnDuplicationMergeScenarios,fnImportMergeScenarios,
				textualConfUnmerge,textualConfSsmerge,
				fpOrderingConf,fpRenamingConf,
				fnDuplicationMissed,fnImportMissed]

			rows.add(newRow.join(' '))
		}

		//printing the result file
		def out = new File('results/resultFPFNAnalysis.csv')
		// deleting old files if it exists
		out.delete()
		out = new File('results/resultFPFNAnalysis.csv')
		rows.each {
			out.append it
			out.append '\n'
		}

		//publishResults()
	}

	def private static publishResults(){
		try{
			def command = "\"C:\\Program Files\\R\\R-3.1.3\\bin\\Rscript.exe\" \"C:\\GGTS\\ggts-bundle\\workspace\\GitCE\\processResultsScript.r\""
			Runtime run = Runtime.getRuntime()
			Process pr = run.exec(command)
			new AntBuilder().copy(todir:"C:\\Users\\Guilherme\\Google Drive\\Pós-Graduação\\Pesquisa\\Outros\\ISQFIEDMA_results", overwrite:true) {fileset(dir:"C:\\GGTS\\ggts-bundle\\workspace\\GitCE\\results\\html" , defaultExcludes: false)}
			new AntBuilder().copy(file:"C:\\GGTS\\ggts-bundle\\workspace\\GitCE\\results\\html\\resultFPFN.html", todir:"C:\\Users\\Guilherme\\Google Drive\\Pós-Graduação\\Pesquisa\\Outros\\ISQFIEDMA_results", overwrite:true)
		} catch(Exception e){
			e.printStackTrace()
		}
	}

	public static void main (String[] args){
		//restoreGitRepositories()
		ArrayList<Project> projects = readProjects();
		runFPFNAnalysis(projects)
		//runWithCommitCsv()
		//publishResults()
	}
}
