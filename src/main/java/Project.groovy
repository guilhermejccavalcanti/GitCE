import java.util.ArrayList;


class Project {
	
	String name
	String url
	String graph
	ArrayList<MergeCommit> listMergeCommit
	
	def setMergeCommits(mergeCommits){
		this.listMergeCommit = mergeCommits
	}

}
