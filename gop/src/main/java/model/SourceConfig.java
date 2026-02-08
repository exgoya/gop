package model;

public class SourceConfig {
	public Integer schemaVersion;
	public String configPath;
	public String source;
	public JdbcSource jdbcSource;
	public Measure[] measure;
	public MeasureV2[] measureV2;
}
