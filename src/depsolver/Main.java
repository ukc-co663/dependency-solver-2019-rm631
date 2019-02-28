package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Things below here are things I've imported...
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.*;

class Package {
	private String name;
	private String version;
	private Integer size;
	private List<List<String>> depends = new ArrayList<>();
	private List<String> conflicts = new ArrayList<>();

	public String getName() { return name; }
	public String getVersion() { return version; }
	
	public Integer getSize() { return size; }
	
	public List<List<String>> getDepends() { return depends; }
	public List<String> getConflicts() { return conflicts; }
	
	public void setName(String name) { this.name = name; }
	public void setVersion(String version) { this.version = version; }
	public void setSize(Integer size) { this.size = size; }
	public void setDepends(List<List<String>> depends) { this.depends = depends; }
	public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }
}

public class Main {
	private static HashSet<List<String>> seenSet = new HashSet<>();
	private static HashMap<String, Integer> solutions = new HashMap<>();
	private static List<String> commands = new ArrayList<>();
	private static boolean solutionFound;
	
	public static void main(String[] args) throws IOException {
		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
		List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
		List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
		List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
		
		solutionFound = false;
		search(initial, repo, constraints);
		//System.out.println(Arrays.asList(commands));
		//System.out.println("Solutions: " + Arrays.asList(solutions));
		printCheapestSolution();
	}
	
	private static void search(List<String> set, List<Package> repo, List<String> constraints) {
		//if(solutionFound) { return; }
		//System.out.println("search() called");
		//System.out.println(Arrays.toString(set.toArray()));
		//System.out.println("Search set printed!");
		if(!valid(set, repo)) { ;return; }
		System.out.println("Set valid");
		if(seenSet.contains(set)) { ;return; }
		//System.out.println("Set not seen before");
		if(finalState(set, constraints)) {
			solutionFound = true;
			//System.out.println("/***************A SOLUTION HAS BEEN FOUND!***************/");
			//System.out.println(JSON.toJSON(commands));
			int costOfSolution = solutionCost(repo);
			//System.out.println("commands non json: "+ commands);
			
			String tmp = "";
			for(String s : commands) {
				tmp+= s+",";
			}
			
			solutions.put(tmp, (Integer)costOfSolution);
			//System.out.println(Arrays.asList(solutions));
			return;			
		}
		//System.out.println("Not the finalState!");
		seenSet.add(set);
		List<String> newSet = new ArrayList<>();
		newSet = set;
		for (Package p : repo) {
			//if(solutionFound) { return; }
			//System.out.println("new p: " + p.getName() + "=" + p.getVersion());
			if(!newSet.contains(p.getName() + "=" + p.getVersion())) {
				newSet.add(p.getName() + "=" + p.getVersion());
				commands.add("+" + p.getName() + "=" + p.getVersion());
				//System.out.println("Installing: " + p.getName() + "=" + p.getVersion());
				
				search(newSet, repo, constraints);
				//if(!solutionFound) {
					//System.out.println("Something has returned us here! Removing last item!");
					newSet.remove(p.getName() + "=" + p.getVersion());
					commands.remove("+" + p.getName() + "=" + p.getVersion());
				//}
				
			} /*else {
				newSet.remove(p.getName() + "=" + p.getVersion());
				System.out.println("Uninstalling: " + p.getName() + "=" + p.getVersion());
				search(newSet, repo, constraints);
			}*/
		}
		if(!solutionFound) {
			// this is the catch for when initial has something that is a conflict 
			// for something essential in the repo
			// TODO: THIS!
			// How do we know the cost of uninstalling something if it didnt start in the repo?
		}
		//System.out.println(Arrays.asList(commands));
		//System.out.println("Solutions: " + Arrays.asList(solutions));
	}
	
	private static boolean valid(List<String> set, List<Package> repo) {
		//System.out.println("valid() called");
		for(Package p : repo) {
			// if p is in the set, then we need its dependencies/conflicts
			if(set.contains(p.getName() + "=" + p.getVersion())) {
				//if(p.getDepends().size() < 1) { System.out.println("No deps"); }
				for(List<String> clause : p.getDepends()) {
					boolean foundDep = false; // we only need one from each line
					//System.out.println("Checking deps");
					for(String q : clause) {
						if(!foundDep) {
							for(String s : set) {
								//System.out.println("q: " + q + " | s: " + s);
								// TODO ADD HANDLING FOR VERSIONS!!
								String[] depSplit = splitPackage(q);
								String compareSymbol = depSplit[2];
								String[] sSplit = splitPackage(s);
								if(depSplit[0].equals(sSplit[0])) {
									//System.out.println("compare symbol: " + compareSymbol);
									switch(compareSymbol) {
										case "=" :
											if(q.equals(s)) {
												foundDep =  true;
											}
											break;
										case "<" :
										// if depSplit >= sSplit then version installed in set is less than the version specified and we good
											if(versionCompare(depSplit[1], sSplit[1])) {
												foundDep =  true;
											}
											break;
										case "<=" :
											if(versionCompare(depSplit[1], sSplit[1])) {
												foundDep =  true;
											}
											break;
										case ">" :
											if(versionCompare(sSplit[1], depSplit[1])) {
												foundDep =  true;
											}
											break;
										case ">=" :
											if(versionCompare(sSplit[1], depSplit[1])) {
												foundDep =  true;
											}
											break;
										default :
											// if theres no symbol we've already found the dep :)
											foundDep = true;
											break;
									}
								}
							}
						}
					}
					if(!foundDep) {
						// WE'RE MISSING A DEPENDENCY!!! ABORT! ABORT! ABORT!
						//System.out.println("Missing dependencies");
						return false;
					}
				}
				boolean foundConflict = false;
				// TODO ADD HANDLING FOR VERSIONS!!
				//System.out.println("Checking conflicts");
				//if(p.getConflicts().size() > 1) { System.out.println("No conflicts"); }
				for(String s : p.getConflicts()) {
					if(!foundConflict) {
						for(String t : set) {
							//System.out.println("s: " + s + " | t: " + t);
							String[] consSplit = splitPackage(s);
							String compareSymbol = consSplit[2];
							String[] tSplit = splitPackage(t);
							if(consSplit[0].equals(tSplit[0])) {
								//System.out.println("compare symbol: " + compareSymbol);
								switch(compareSymbol) {
									case "=" :
										if(s.equals(t)) {
											foundConflict = true;
										}
										break;
									case "<" :
										if(versionCompare(consSplit[1], tSplit[1])) {
											foundConflict =  true;
										}
										break;
									case "<=" :
										if(versionCompare(consSplit[1], tSplit[1])) {
											foundConflict =  true;
										}
										break;
									case ">" :
										if(versionCompare(tSplit[1], consSplit[1])) {
											foundConflict =  true;
										}
										break;
									case ">=" :
										if(versionCompare(tSplit[1], consSplit[1])) {
											foundConflict =  true;
										}
										break;
									default :
										foundConflict = true;
										break;
								}
							}
						}
					}
				}
				if(foundConflict) {
					// THERE IS A CONFLICT!!! ABORT! ABORT! ABORT!
					//System.out.println("Conflict found");
					return false;
				}
			}
		}
		return true;
	}
	
	private static boolean finalState(List<String> set, List<String> constraints) {
		//System.out.println("finalState() called");
		for(String s : constraints) {
			//System.out.println("constraint s: " + s);
			String symbol = Character.toString(s.charAt(0));
			s = s.substring(1); 
			String[] consNameVers = splitPackage(s); 
			String consName = consNameVers[0];
			String consVersion = consNameVers[1];
			String compareSymbol = consNameVers[2]; // =, <, <=, >, >=
			
			boolean constraintMet = false;
			if(symbol.equals("+")) {
				for(String p : set) {
					//System.out.println("string p: "+ p);
					String[] pSplit = splitPackage(p);
					if(consName.equals(pSplit[0])) {
						//System.out.println("compare symbol: " + compareSymbol);
						switch(compareSymbol) {
							case "=" :
								if(consVersion.equals(pSplit[1])) {
									constraintMet = true;
								}
								break;
							case "<" :
								
								break;
							case "<=" :
							
								break;
							case ">" :
							
								break;
							case ">=" :
							
								break;
							default :
								constraintMet = true;
								break;
						}
					}
				}
			}
			if(!constraintMet) {
				return false;
			}
			if(symbol.equals("-")) {
				int i = 0;
				for(String p : set) {
					String[] pSplit = splitPackage(p);
					switch(compareSymbol) {
						case "=" :
							if(consName.equals(pSplit[0]) && consVersion.equals(pSplit[1])) {
								i++;
							}
							break;
						case "<" :
						
							break;
						case "<=" :
						
							break;
						case ">" :
						
							break;
						case ">=" :
						
							break;
						default :
							break;
					}
				}
				if(i > 0) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 *	return true if vers1 is >= vers2	
	 */
	private static boolean versionCompare(String vers1, String vers2) {
		vers1 = vers1.replace("0", "");
		vers2 = vers2.replace("0", "");
		// casting arrays.aslist as an arraylist causes "uses unchecked or unsafe operations"
		List<String> vers1Arr = new ArrayList(Arrays.asList(vers1.split("\\.")));
		List<String> vers2Arr = new ArrayList(Arrays.asList(vers2.split("\\.")));
		
		while(vers1Arr.size() != vers2Arr.size()) { // artifically add 0s to pad the size diff
			if(vers1Arr.size() > vers2Arr.size()) {
				vers2Arr.add("0");
			} else {
				vers1Arr.add("0");
			}
		}
		
		for(int i = 0; i < vers1Arr.size(); i++) {
			if(Integer.parseInt(vers1Arr.get(i)) > Integer.parseInt(vers2Arr.get(i))) {
				return true;
			}
			if(Integer.parseInt(vers2Arr.get(i)) > Integer.parseInt(vers1Arr.get(i))) {
				return false;
			}
		}
		return true; // this *should* be if they're equal
	}
	
	private static String[] splitPackage(String s) {
		String[] consNameVers = new String[2]; 
		String consName = "";
		String consVersion = "";
		String compareSymbol = "";
		
		if(s.contains("=")) {
			consNameVers = s.split("=");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = "=";
		} else if(s.contains("<")) {
			consNameVers = s.split("<");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = "<";
		} else if(s.contains("<=")) {
			consNameVers = s.split("<=");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = "<=";
		} else if(s.contains(">")) {
			consNameVers = s.split(">");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = ">";
		} else if(s.contains(">=")) {
			consNameVers = s.split(">=");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = ">=";
		} else {
			// this is the catch all in case something with no version is passed here eg "D"
			consName = s;
		}
		String[] result = {
			consName, consVersion, compareSymbol
		};
		return result;
	}
	
	private static int solutionCost(List<Package> repo) {
		int result = 0;
		
		for(Package p : repo) {
			String nameVers = p.getName() + "=" + p.getVersion();
			for(String s : commands) {
				s = s.substring(1); // remove the symbol
				if(s.equals(nameVers)) {
					result += p.getSize();
				}
			}
		}
		
		return result;
	}
	
	private static void printCheapestSolution() {
		List<String> result = new ArrayList<>();
		int resultCost = 0;
		//System.out.println("result.size() (should be 0): " + result.size());
		//System.out.println("solutions.size(): " + solutions.size());
		//System.out.println(Arrays.asList(solutions));
		for(Map.Entry<String, Integer> entry : solutions.entrySet()) {
			//System.out.println(entry.getKey());
			if(entry.getValue() < resultCost || result.size() == 0) {
				result = Arrays.asList(entry.getKey().split(","));
				resultCost = entry.getValue();
			}
		}
		//System.out.println("/*******Cheapest Solution*******/");
		System.out.println(JSON.toJSON(result));
	}
	
	static String readFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		StringBuilder sb = new StringBuilder();
		br.lines().forEach(line -> sb.append(line));
		return sb.toString();
	}
}