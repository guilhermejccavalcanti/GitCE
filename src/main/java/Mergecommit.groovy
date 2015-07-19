
class MergeCommit {
	
	String sha
	String parent1
	String parent2
	String ancestor
	String projectName
	String projectURL
	String graph
	String revisionFile
	
	def String toString(){
		return 'SHA= ' + this.sha + ', Parent1= ' +this.parent1+', Parent2= ' +this.parent2+ ', Ancestor= ' +this.ancestor + ', Project= ' +this.projectName + ', Graph= ' +this.graph+ ', File= ' + this.revisionFile
		
	}

}
