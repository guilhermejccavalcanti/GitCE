import java.util.ArrayList;


class Printer {

	public void writeCSV(ArrayList<MergeCommit> listMC){
		def out = new File('commits.csv')

		// deleting old files if it exists
		out.delete()

		out = new File('commits.csv')

		def firstRow = ["Merge commit", "Parent 1", "Parent 2"]
		out.append firstRow.join(',')
		out.append '\n'

		listMC.each {
			def row = [it.sha, it.parent1, it.parent2]
			out.append row.join(',')
			out.append '\n'
		}
	}

	public void writeNumberOfMergeCommits(ArrayList<MergeCommit> listMC){
		def out = new File('log_number_of_mergecommits.csv')
		if(!listMC.isEmpty()){
			def row = [listMC.get(0).projectName,listMC.size()]
			out.append row.join(',')
			out.append '\n'
		}
	}
}
