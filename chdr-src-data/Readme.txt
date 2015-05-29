Steps to deploy new source content:

	1) Place the source files into the native-source folder
	2) Update the version number as appropriate in pom.xml
	3) Run a command like this to deploy - (VA is a server name that must be defined with credentials in your maven configuration):
		mvn deploy -DaltDeploymentRepository=maestro::default::https://va.maestrodev.com/archiva/repository/data-files/
		
Note - new source content should not be checked into SVN.  When finished, simply empty the native-source folder.

For CHDR - the loader currently expects 3 different Excel files (*.xls)
	[stuff] Drug Products Release 63.xls
	[stuff] Reactants Release 63.xls
	[stuff] Reactions Release 63.xls
	
The version number on the end doesn't matter, but the prefix must be right.

Alternatively, it can read the following:

	Drug Products Release 61-Outgoing.csv
	Drug Products Release 61-Incoming.csv
	Reactants Release 61-Outgoing.csv
	Reactants Release 61-Incoming.csv
	Reactions Release 61-Outgoing.csv
	Reactions Release 61-Incoming.csv
	
