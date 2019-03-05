// TODO: The checks for whether things are < or > also work if <= or >=, add a check for = into the former 

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
	private static HashMap<String, Integer> biggestConflicts = new HashMap<>(); // s = package, int = no of conflicts caused
	private static List<String> commands = new ArrayList<>();
	private static boolean solutionFound = false;;
	private static boolean initialSet = true;
	
	public static void main(String[] args) throws IOException {
		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
		List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
		List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
		List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
		//System.out.println(initial.size());
		/**
		 *	The brief doesn't outright say that the initial set will be valid
		 *	but that all intermediate states must be
		 *	Regardless, if the initial has *a lot* of problems
		 *	then it stands no chance of being valid
		 *	hence we'll make sure the initial set is valid before we do anything else...
		 */
		while(!valid(initial,repo)) {
			initial = removeProblemPackages(initial, repo);
		}
		//System.out.println(commands.size());
		search(initial, repo, initial, constraints);
		printCheapestSolution();
	}
	
	private static void search(List<String> set, List<Package> repo, List<String> initial, List<String> constraints) {
		//System.out.println(commands.size());
		//System.out.println(set.size());
		//if(solutionFound) { return; } // a catch all in case anything tries to search after we have our solution
		//System.out.println("search() called");
		//System.out.println(Arrays.toString(set.toArray()));
		//System.out.println("Search set printed!");
		if(!valid(set, repo)) { 
			/*if(initialSet) {
				initialSet = false;
				search(removeProblemPackages(set, repo), repo, constraints);
			}*/
			return; 
		}
		//System.out.println("Set valid");
		if(seenSet.contains(set)) { return; }
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
		//List<String> newSet = new ArrayList<>();
		//newSet = set;
		for (Package p : repo) {
			//if(solutionFound) { return; }
			//System.out.println("new p: " + p.getName() + "=" + p.getVersion());
			if(!set.contains(p.getName() + "=" + p.getVersion()) && !commands.contains("-" + p.getName() + "=" + p.getVersion())) {
				set.add(p.getName() + "=" + p.getVersion());
				commands.add("+" + p.getName() + "=" + p.getVersion());
				//System.out.println("Installing: " + p.getName() + "=" + p.getVersion());
				
				search(set, repo, initial, constraints);
				//if(!solutionFound) {
					//System.out.println("Something has returned us here! Removing last item!");
				set.remove(p.getName() + "=" + p.getVersion());
				commands.remove("+" + p.getName() + "=" + p.getVersion());
				//}
				
				//search(set, repo, initial, constraints);
				
			} else if(initial.contains(p.getName() + "=" + p.getVersion())) {
				// uninstall stuff from set that was part of initial set and in the repo
				set.remove(p.getName() + "=" + p.getVersion());
				commands.add("-" + p.getName() + "=" + p.getVersion());
				search(set, repo, initial, constraints);
				set.add(p.getName() + "=" + p.getVersion());
				commands.remove("-" + p.getName() + "=" + p.getVersion());
			}
			/*else {
				newSet.remove(p.getName() + "=" + p.getVersion());
				System.out.println("Uninstalling: " + p.getName() + "=" + p.getVersion());
				search(newSet, repo, constraints);
			}*/
			/*if(set.contains(p.getName() + "=" + p.getVersion())) {
				set.remove(p.getName() + "=" + p.getVersion());
				commands.add("-" + p.getName() + "=" + p.getVersion());
				search(set, repo, initial, constraints);
				set.add(p.getName() + "=" + p.getVersion());
				commands.remove("-" + p.getName() + "=" + p.getVersion());
			}*/
		}
		//if(!solutionFound) {
			// this is the catch for when a solution hasn't been found
			// we need to remove things from the set and try again!
			//System.out.println("Solution not found!");
			//set = removeBiggestConflict(set);
			//search(set, repo, initial, constraints);
		//}
		//System.out.println(Arrays.asList(commands));
		//System.out.println("Solutions: " + Arrays.asList(solutions));
	}
	
	private static List<String> removeBiggestConflict(List<String> set) {
		if(biggestConflicts.size() > 1) {
			String max = Collections.max(biggestConflicts.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
		
			set.remove(max);
		}
		return set;
	}
	
	/**
	 *	This will go through the repo and remove things from it, each time it does, it will call search
	 */
	private static void searchInverse(List<String> set, List<Package> repo, List<String> constraints) {
		if(!valid(set, repo)) { return; }
		if(seenSet.contains(set)) { return; }
		seenSet.add(set);
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
		/*for(Package p : repo) {
			if(newSet.contains(p.getName() + "=" + p.getVersion())) {
				newSet.remove(p.getName() + "=" + p.getVersion());
				commands.remove("+" + p.getName() + "=" + p.getVersion());
				search(newSet, repo, constraints);
				newSet.add(p.getName() + "=" + p.getVersion());
				commands.add("+" + p.getName() + "=" + p.getVersion());
			}
		}*/
		Iterator<String> it = set.iterator();
		while(it.hasNext()) {
			String tmp = it.next();
			it.remove();
			commands.add("-" + tmp);
			for(Package p : repo) {
				String q = p.getName() + "=" + p.getVersion();
				if(!q.equals(tmp)) {
					set.add(p.getName() + "=" + p.getVersion());
					commands.add("+" + p.getName() + "=" + p.getVersion());
				}
			}
			set.add(tmp);
			commands.remove("-" + tmp);
		}
	}
	
	private static boolean valid(List<String> set, List<Package> repo) {
		//System.out.println("valid() called");
		for(Package p : repo) {
			// if p is in the set, then we need its dependencies/conflicts
			if(set.contains(p.getName() + "=" + p.getVersion())) {
				//if(p.getDepends().size() < 1) { System.out.println("No deps"); }
				for(List<String> clause : p.getDepends()) {
					boolean foundDep = false; // we only need one from each line
					//System.out.println("Checking deps of: " + p.getName() + "=" + p.getVersion());
					for(String q : clause) {
						String[] depSplit = splitPackage(q);
						String compareSymbol = depSplit[2];
						if(!foundDep) {
							for(String s : set) {
								//System.out.println("q: " + q + " | s: " + s);
								
								
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
											if(!depSplit[1].equals(sSplit[1])) {
												if(versionCompare(depSplit[1], sSplit[1])) {
													foundDep =  true;
												}
											}
											break;
										case "<=" :
											if(versionCompare(depSplit[1], sSplit[1])) {
												foundDep =  true;
											}
											break;
										case ">" :
											if(!depSplit[1].equals(sSplit[1])) {
												if(versionCompare(sSplit[1], depSplit[1])) {
													foundDep =  true;
												}
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
				//System.out.println("Checking conflicts");
				//if(p.getConflicts().size() > 1) { System.out.println("No conflicts"); }
				for(String s : p.getConflicts()) {
					String[] consSplit = splitPackage(s);
					String compareSymbol = consSplit[2];
					if(!foundConflict) {
						for(String t : set) {
							//System.out.println("s: " + s + " | t: " + t);
							String[] tSplit = splitPackage(t);
							if(consSplit[0].equals(tSplit[0])) {
								//System.out.println("compare symbol: " + compareSymbol);
								switch(compareSymbol) {
									case "=" :
										if(s.equals(t)) {
											foundConflict = true;
											Integer value = biggestConflicts.get(t);
											if(value != null) {
												biggestConflicts.put(t, value++);
											} else {
												biggestConflicts.put(t, 1);
											}
										}
										break;
									case "<" :
										if(!consSplit[1].equals(tSplit[1])) {
											if(versionCompare(consSplit[1], tSplit[1])) {
												foundConflict =  true;
												Integer value = biggestConflicts.get(t);
												if(value != null) {
													biggestConflicts.put(t, value++);
												} else {
													biggestConflicts.put(t, 1);
												}
											}
										}
										break;
									case "<=" :
										if(versionCompare(consSplit[1], tSplit[1])) {
											foundConflict =  true;
											Integer value = biggestConflicts.get(t);
											if(value != null) {
												biggestConflicts.put(t, value++);
											} else {
												biggestConflicts.put(t, 1);
											}
										}
										break;
									case ">" :
										if(!consSplit[1].equals(tSplit[1])) {
											if(versionCompare(tSplit[1], consSplit[1])) {
												foundConflict =  true;
												Integer value = biggestConflicts.get(t);
												if(value != null) {
													biggestConflicts.put(t, value++);
												} else {
													biggestConflicts.put(t, 1);
												}
											}
										}
										break;
									case ">=" :
										if(versionCompare(tSplit[1], consSplit[1])) {
											foundConflict =  true;
											Integer value = biggestConflicts.get(t);
											if(value != null) {
												biggestConflicts.put(t, value++);
											} else {
												biggestConflicts.put(t, 1);
											}
										}
										break;
									default :
										foundConflict = true;
										Integer value = biggestConflicts.get(t);
										if(value != null) {
											biggestConflicts.put(t, value++);
										} else {
											biggestConflicts.put(t, 1);
										}
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
	
	private static List<String> removeProblemPackages(List<String> set, List<Package> repo) {
		//System.out.println("removeProblemPackages() called");
		List<String> removalList = new ArrayList<>();
		
		int counter = 0;
		int max = set.size();
		for(Package p : repo) {
			if(counter >= max) { break; }
			boolean packageRemoved = false;
			// if p is in the set, then we need its dependencies/conflicts
			if(set.contains(p.getName() + "=" + p.getVersion())) {
				counter++;
				//if(p.getDepends().size() < 1) { System.out.println("No deps"); }
				for(List<String> clause : p.getDepends()) {
					boolean foundDep = false; // we only need one from each line
					//System.out.println("Checking deps");
					for(String q : clause) {
						String[] depSplit = splitPackage(q);
						String compareSymbol = depSplit[2];
						if(!q.contains("=")) {
							if(!foundDep) {
								for(String s : set) {
									//System.out.println("q: " + q + " | s: " + s);
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
												if(!depSplit[1].equals(sSplit[1])) {
													if(versionCompare(depSplit[1], sSplit[1])) {
														foundDep =  true;
													}
												}
												break;
											case "<=" :
												if(versionCompare(depSplit[1], sSplit[1])) {
													foundDep =  true;
												}
												break;
											case ">" :
												if(!depSplit[1].equals(sSplit[1])) {
													if(versionCompare(sSplit[1], depSplit[1])) {
														foundDep =  true;
													}
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
						} else {
							if(set.contains(q)) {
								foundDep = true;
							}
						}
					}
					if(!foundDep) {
						// WE'RE MISSING A DEPENDENCY!!! PURGE THE PACKAGE!
						//removalList.add(p.getName() + "=" + p.getVersion());
						packageRemoved = true;
						set.remove(p.getName() + "=" + p.getVersion());
						commands.add("-"+ p.getName() + "=" + p.getVersion());
					}
				}
				if(!packageRemoved) {
					boolean foundConflict = false;
					//System.out.println("Checking conflicts");
					//if(p.getConflicts().size() > 1) { System.out.println("No conflicts"); }
					for(String s : p.getConflicts()) {
						String[] consSplit = splitPackage(s);
						String compareSymbol = consSplit[2];
						if(!foundConflict) {
							for(String t : set) {
								//System.out.println("s: " + s + " | t: " + t);
								String[] tSplit = splitPackage(t);
								if(consSplit[0].equals(tSplit[0])) {
									//System.out.println("compare symbol: " + compareSymbol);
									switch(compareSymbol) {
										case "=" :
											if(s.equals(t)) {
												foundConflict = true;
												//removalList.add(t);
											}
											break;
										case "<" :
											if(!consSplit[1].equals(tSplit[1])) {
												if(versionCompare(consSplit[1], tSplit[1])) {
													foundConflict =  true;
													//removalList.add(t);
												}
											}
											break;
										case "<=" :
											if(versionCompare(consSplit[1], tSplit[1])) {
												foundConflict =  true;
												//removalList.add(t);
											}
											break;
										case ">" :
											if(!consSplit[1].equals(tSplit[1])) {
												if(versionCompare(tSplit[1], consSplit[1])) {
													foundConflict =  true;
													//removalList.add(t);
												}
											}
											break;
										case ">=" :
											if(versionCompare(tSplit[1], consSplit[1])) {
												foundConflict =  true;
												//removalList.add(t);
											}
											break;
										default :
											foundConflict = true;
											//removalList.add(t);
											break;
									}
								}
							}
						}
					}
					if(foundConflict) {
						// THERE IS A CONFLICT!!! PURGE THE PACKAGE!
						removalList.add(p.getName() + "=" + p.getVersion());
						set.remove(p.getName() + "=" + p.getVersion());
						commands.add("-"+ p.getName() + "=" + p.getVersion());
					}
				}
			}
		}
		
		//for(String s : removalList) {
			// the removalList could possible contain dupes, maybe an arraylist wasnt the best data
			// structure for this, but its a simple check to prevent shit hitting the fan...
			//if(set.contains(s)) {
				//set.remove(s);
				//commands.add("-" + s);
			//}
		//}
		//System.out.println(commands);
		return set;
	}
	
	/**
	 *	return true if vers1 is >= vers2	
	 *	returning true or false here turned out to be a small
	 *	problem since we can't distinguish between
	 * 	<,> and <=,>=. Rather than change this
	 *	to return some arbitary int for each one
	 *	I thought it woould be easier and make more sense
	 *	to just add in a quick check for if they're 
	 *	equal before calling this method
	 */
	private static boolean versionCompare(String vers1, String vers2) {
		String vers1Tmp = vers1.replace("0", "");
		String vers2Tmp = vers2.replace("0", "");
		if(!vers1Tmp.isEmpty()) {
			vers1 = vers1Tmp;
		}
		if(!vers2Tmp.isEmpty()) {
			vers2 = vers2Tmp;
		}
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
		//System.out.println("splitPackage: " + s);
		if(s.contains("<=")) {
			s = s.replace("<", "");
			consNameVers = s.split("=");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = "<=";
		} else if(s.contains("<")) {
			consNameVers = s.split("<");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = "<";
		} else if(s.contains(">=")) {
			s = s.replace(">", "");
			consNameVers = s.split("=");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = ">=";
		} else if(s.contains(">")) {
			consNameVers = s.split(">");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = ">";
		} else if(s.contains("=")) {
			consNameVers = s.split("=");
			consName = consNameVers[0];
			consVersion = consNameVers[1];
			compareSymbol = "=";
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
		
		/*for(Package p : repo) {
			String nameVers = p.getName() + "=" + p.getVersion();
			for(String s : commands) {
				s = s.substring(1); // remove the symbol
				if(s.equals(nameVers)) {
					result += p.getSize();
				}
			}
			System.out.println();
		}
		
		System.out.println(result+"\n");*/
		
		for(String s : commands) {
			//System.out.println(s);
			if(s.contains("-")) {
				result += 1000000;
			} else {
				for(Package p : repo) {
					String nameVers = p.getName() + "=" + p.getVersion();
					String q = s.substring(1); // remove the symbol
					if(q.equals(nameVers)) {
						result += p.getSize();
					}
				}
			}
		}
		
		//if(result == 0) { return 1000000; }
		return result;
	}
	
	private static void printCheapestSolution() {
		//System.out.println("solsize(): " + solutions.size());
		//System.out.println("biggestConflicts: " + biggestConflicts.size());
		List<String> result = new ArrayList<>();
		int resultCost = 0;
		//System.out.println("result.size() (should be 0): " + result.size());
		//System.out.println("solutions.size(): " + solutions.size());
		//System.out.println(Arrays.asList(solutions));
		for(Map.Entry<String, Integer> entry : solutions.entrySet()) {
			//System.out.println(entry.getKey());
			//System.out.println("resultCost: " + entry.getValue());
			if(entry.getValue() < resultCost || result.size() == 0) {
				result = Arrays.asList(entry.getKey().split(","));
				resultCost = entry.getValue();
			}
		}
		//System.out.println("/*******Cheapest Solution*******/");
		System.out.println(JSON.toJSON(result));
		//System.out.println(resultCost);
	}
	
	static String readFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		StringBuilder sb = new StringBuilder();
		br.lines().forEach(line -> sb.append(line));
		return sb.toString();
	}
}