package cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandLineParser {
	List<String> args = new ArrayList<>();
	HashMap<String, List<String>> map = new HashMap<>();
	Set<String> flags = new HashSet<>();

	public CommandLineParser(String arguments[]) {
		this.args = Arrays.asList(arguments);
		map();
	}

	// Return argument names
	public Set<String> getArgumentNames() {
		Set<String> argumentNames = new HashSet<>();
		argumentNames.addAll(flags);
		argumentNames.addAll(map.keySet());
		return argumentNames;
	}

	// Check if flag is given
	public boolean getFlag(String flagName) {
		if (flags.contains(flagName))
			return true;
		return false;
	}

	public int getArgumentValueInt(String argumentName) {
		if (getArgumentValue(argumentName)[0] != null) {
			return Integer.parseInt(getArgumentValue(argumentName)[0]);
		} else {
			return 0;
		}
	}

	// Return argument value for particular argument name
	public String[] getArgumentValue(String argumentName) {
		if (map.containsKey(argumentName))
			return map.get(argumentName).toArray(new String[0]);
		else
			return new String[2];
	}

	// Map the flags and argument names with the values
	public void map() {
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if (args.indexOf(arg) == (args.size() - 1)) {
					flags.add(arg.replace("-", ""));
				} else if (args.get(args.indexOf(arg) + 1).startsWith("-")) {
					flags.add(arg.replace("-", ""));
				} else {
					// List of values (can be multiple)
					List<String> argumentValues = new ArrayList<>();
					int i = 1;
					while (args.indexOf(arg) + i != args.size() && !args.get(args.indexOf(arg) + i).startsWith("-")) {
						argumentValues.add(args.get(args.indexOf(arg) + i));
						i++;
					}
					map.put(arg.replace("-", ""), argumentValues);
				}
			}
		}
	}

	public static void printHelp() {
		System.out.println(" ---");
		System.out.println(" gop server -config <config file path> [options]");
		System.out.println(" gop run -config <config file path> [options]");
		System.out.println(" gop ls [<config>[/<source>[/YYYY[/MM]]]] [-path <log root>]");
		System.out.println(" gop watch [-config <config name>] [-source <sourceId>] [tail] [options]");
		System.out.println(" gop init");
		System.out.println(" gop version | gop -version");
		System.out.println(" ");
		System.out.println(" run option:");
		System.out.println("   -interval <sec>   (default unit)");
		System.out.println("   -interval-ms <ms>");
		System.out.println("   * run mode is continuous (Ctrl+C to stop)");
		System.out.println(" ");
		System.out.println(" ls option:");
		System.out.println("   gop ls                         (list configs)");
		System.out.println("   gop ls <config>                 (list sources)");
		System.out.println("   gop ls <config>/<source>        (list years)");
		System.out.println("   gop ls <config>/<source>/YYYY   (list months)");
		System.out.println("   gop ls <config>/<source>/YYYY/MM (list logs)");
		System.out.println("   -path <log root path>  (default: data/ or GOP_LOG_PATH)");
		System.out.println(" ");
		System.out.println(" watch option:");
		System.out.println("   -config <config name>");
		System.out.println("   -source <sourceId>");
		System.out.println(
				"   [tail] [ -time 'yyyy-mm-dd hh24:mi:ss.fff' 'yyyy-mm-dd hh24:mi:ss.fff' | -name <column name> | -tag <tag name> ]");
		System.out.println("   [ -head | -tail <print count> ]");
		System.out.println("   [ -f <log file path> ]");
		System.out.println("   [ -follow | -F ]");
		System.out.println("   [ -path <log root path> ]");
		System.out.println("   [ -csv ]");
		System.out.println(" ");
		System.out.println(" ---");
		System.out.println(" sample use");
		System.out.println(" gop server -config /opt/gop/config/mysql.json");
		System.out.println(" gop run -config /opt/gop/config/mysql.json");
		System.out.println(" gop init");
		System.out.println(
				" gop ls");
		System.out.println(
				" gop ls config-mysql/mysql-local/2026");
		System.out.println(
				" gop watch -source mysql-local -time '2022-12-01 03:14:40.000' '2022-12-01 03:15:00.000'");
		System.out.println(
				" gop watch -source mysql-local -name execute -tail 10");
		System.out.println(
				" gop watch -source mysql-local -tag tag1 -head 10");
		System.out
				.println(" gop watch -source mysql-local");
		System.out
				.println(" gop watch -config config-multi");
		System.out
				.println(" gop watch -source mysql-local tail -follow");
		System.out.println(" ");
    }
}
