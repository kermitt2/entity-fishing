The dataset can be obtained for research purpose from NIST.

Reuters files are available in XML format. 
To convert these files into the expected txt format with the right offsets compatible with the annotations, use the shell command:
>  for file in data/corpus/corpus-long/aida/RawText/*.xml; do xmllint --xpath "//newsitem//text()" "$file" > "${file/%xml/txt}"; done
and ignore the two first lines of the resulting .txt file

