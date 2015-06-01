import java.util.ArrayList;

class Read {

	private ArrayList<MergeCommit> listMergeCommit
	private ArrayList<Project> listProject
	private String csvCommitsFile
	private String csvProjectsFile

	public Read(fileName){
		this.listMergeCommit = new ArrayList<MergeCommit>()
		this.listProject = new ArrayList<Project>()
		this.csvProjectsFile = fileName
		this.readProjectsCSV()
	}
	
	def setCsvCommitsFile(fileName){
		this.csvCommitsFile = fileName
	}

	def readCommitsCSV() {
		BufferedReader br = null
		String line = ""
		String csvSplitBy = ","
		try {
			br = new BufferedReader(new FileReader(this.csvCommitsFile))
			br.readLine()
			while ((line = br.readLine()) != null) {
				String[] shas = line.split(csvSplitBy)

				def mergeCommit = new MergeCommit()
				mergeCommit.sha 	= shas[0]
				mergeCommit.parent1 = shas[1]
				mergeCommit.parent2 = shas[2]

				this.listMergeCommit.add(mergeCommit)

				println ("SHA's [MergeCommit= " + mergeCommit.sha 	+ " , Parent1=" + mergeCommit.parent1 + " , Parent2=" + mergeCommit.parent2 +  "]")
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace()
		} catch (IOException e) {
			e.printStackTrace()
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace()
				}
			}
		}
	}
	
	
	def readProjectsCSV() {
		BufferedReader br = null
		String line = ""
		String csvSplitBy = ","
		try {
			br = new BufferedReader(new FileReader(this.csvProjectsFile))
			br.readLine()
			while ((line = br.readLine()) != null) {
				String[] info = line.split(csvSplitBy)

				def project = new Project()
				project.name 	= info[0]
				project.url = info[1]
				project.graph = info[2]

				this.listProject.add(project)

				println ("PROJECT [Name= " + project.name + " , Url=" + project.url + " , Graph dir=" + project.graph +  "]")
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace()
		} catch (IOException e) {
			e.printStackTrace()
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace()
				}
			}
		}
	}

	def ArrayList<MergeCommit> getMergeCommitsList(){
		return this.listMergeCommit
	}
	
	def ArrayList<Project> getProjects(){
		return this.listProject
	}

	//	static void main(args) {
	//		Reader obj = new Reader("commits.csv")
	//		obj.readCSV()
	//	}
}
