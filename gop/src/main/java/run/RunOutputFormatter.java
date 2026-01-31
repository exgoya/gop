package run;

import java.util.ArrayList;
import java.util.List;

import model.Data;
import model.ResultCommon;

public final class RunOutputFormatter {
	private static boolean RUN_HEADER_PRINTED = false;
	private static int RUN_CYCLE_COUNT = 0;
	private static final int RUN_HEADER_EVERY = 10;

	private RunOutputFormatter() {
	}

	public static void printRunSections(List<Data> dataList, boolean printCSV, String cycleTime) {
		if (dataList == null || dataList.isEmpty()) {
			System.out.println("no data!");
			return;
		}
		RUN_CYCLE_COUNT++;
		boolean includeHeader = !RUN_HEADER_PRINTED || (RUN_CYCLE_COUNT % RUN_HEADER_EVERY == 0);
		List<List<String>> sections = new ArrayList<>();
		for (int i = 0; i < dataList.size(); i++) {
			Data data = dataList.get(i);
			sections.add(formatRunSection(data, printCSV, cycleTime, includeHeader, i));
		}
		RUN_HEADER_PRINTED = true;
		if (includeHeader && !printCSV) {
			System.out.println(buildSourcesLine(dataList, sections));
		}
		List<String> lines = flattenSections(sections);
		for (String line : lines) {
			System.out.println(line);
		}
	}

	private static List<String> flattenSections(List<List<String>> sections) {
		List<String> lines = new ArrayList<>();
		if (sections.isEmpty()) {
			return lines;
		}
		int rows = sections.get(0).size();
		for (int r = 0; r < rows; r++) {
			StringBuilder row = new StringBuilder();
			for (int c = 0; c < sections.size(); c++) {
				String cell = sections.get(c).get(r);
				if (c > 0) {
					row.append(" | ");
				}
				row.append(cell);
			}
			lines.add(row.toString());
		}
		return lines;
	}

	private static List<String> formatRunSection(Data data, boolean printCSV, String cycleTime, boolean includeHeader,
			int sectionsIndex) {
		List<String> lines = new ArrayList<>();
		if (printCSV) {
			StringBuilder row = new StringBuilder();
			if (sectionsIndex == 0) {
				row.append(cycleTime);
			}
			if (data.rc != null) {
				for (ResultCommon rc : data.rc) {
					if (rc == null) {
						continue;
					}
					if (row.length() > 0) {
						row.append(",");
					}
					row.append(rc.value);
				}
			}
			if (includeHeader) {
				StringBuilder header = new StringBuilder();
				if (sectionsIndex == 0) {
					header.append("time");
				}
				if (data.rc != null) {
					for (ResultCommon rc : data.rc) {
						if (rc == null) {
							continue;
						}
						if (header.length() > 0) {
							header.append(",");
						}
						header.append(rc.measure);
					}
				}
				lines.add(header.toString());
			}
			lines.add(row.toString());
			return lines;
		}
		List<String> headers = new ArrayList<>();
		List<String> values = new ArrayList<>();
		List<Integer> widths = new ArrayList<>();

		if (sectionsIndex == 0) {
			String timeHeader = "time";
			String timeValue = cycleTime;
			headers.add(timeHeader);
			values.add(timeValue);
			widths.add(Math.max(timeHeader.length(), timeValue.length()));
		}

		if (data.rc != null) {
			for (ResultCommon rc : data.rc) {
				if (rc == null) {
					continue;
				}
				String h = rc.measure == null ? "" : rc.measure;
				String v = String.valueOf(rc.value);
				headers.add(h);
				values.add(v);
				widths.add(Math.max(h.length(), v.length()));
			}
		}

		if (includeHeader) {
			lines.add(joinColumns(headers, widths));
		}
		lines.add(joinColumns(values, widths));
		return lines;
	}

	private static String joinColumns(List<String> cols, List<Integer> widths) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cols.size(); i++) {
			if (i > 0) {
				sb.append("  ");
			}
			sb.append(padRight(cols.get(i), widths.get(i)));
		}
		return sb.toString();
	}

	private static String buildSourcesLine(List<Data> dataList, List<List<String>> sections) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < dataList.size(); i++) {
			String source = dataList.get(i).source == null ? "-" : dataList.get(i).source;
			int width = 0;
			if (i < sections.size()) {
				for (String line : sections.get(i)) {
					width = Math.max(width, line.length());
				}
			}
			if (i > 0) {
				sb.append(" | ");
			}
			sb.append(padRight(source, width));
		}
		return sb.toString();
	}

	private static String padRight(String s, int n) {
		if (s.length() >= n) {
			return s;
		}
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < n) {
			sb.append(" ");
		}
		return sb.toString();
	}
}
