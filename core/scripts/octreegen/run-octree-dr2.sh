DSNAME=001_20190402_dr2-small
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.1 --pllxerrfaint 0.005 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out 
DSNAME=002_20190402_dr2-default
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.2 --pllxerrfaint 0.005 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
DSNAME=003_20190402_dr2-bright
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.9 --pllxerrfaint 0.0 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
DSNAME=004_20190402_dr2-large
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.5 --pllxerrfaint 0.125 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
DSNAME=005_20190402_dr2-verylarge
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.9 --pllxerrfaint 0.9 --hip --pllxzeropoint -0.029 --postprocess --childcount 10000 --parentcount 50000 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
DSNAME=006_20190402_dr2-extralarge
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --hip --pllxzeropoint -0.029 --geodistfile /home/sagrista/gaiadata/DR2/geo_distances/ --postprocess --childcount 10000 --parentcount 50000 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
DSNAME=007_20190402_dr2-ratherlarge
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.5 --pllxerrfaint 0.5 --hip --pllxzeropoint -0.029 --postprocess --childcount 1000 --parentcount 50000 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
DSNAME=008_20190402_dr2-ruwe
nohup ./octreegen --loader CsvCatalogDataProvider --input /home/sagrista/gaiadata/DR2/dr2/csv/ --output /home/sagrista/gaiadata/DR2/dr2/out/$DSNAME/ --maxpart 100000 --ruwe 1.4 --ruwe-file /home/sagrista/gaiadata/DR2/ruwe/ruwes.txt.gz --hip --pllxzeropoint -0.029 --postprocess --childcount 1000 --parentcount 50000 --magcorrections --xmatchfile data/gdr2hip/gdr2-hip-xmatch-all.csv > dr2_logs/$DSNAME.out
