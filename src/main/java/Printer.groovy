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
}
