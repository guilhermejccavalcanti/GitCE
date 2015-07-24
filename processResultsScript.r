deleteAllFiles <- function(exportPath) {
	fileToRemove = paste(exportPath, "resultFPFN.html", sep="")
	if (file.exists(fileToRemove)) {
	file.remove(fileToRemove)
	}
}

main<-function(){
	importPath = "C:/GGTS/workspace/GitCE/results/"
	exportPath = "C:/GGTS/workspace/GitCE/results/html/"

	#all results
	resultFPFNFile= "C:/GGTS/workspace/GitCE/results/resultFPFNAnalysis.csv"

	#HTML file
	htmlFile = paste(exportPath, "resultFPFN.html", sep="")

	#delete previous files
	deleteAllFiles(exportPath)

	#read and edit conflict rate table
	resultFPFNRaw = read.table(file=paste(importPath, resultFPFNFile, sep=""), header=T)
	

}

main()
	