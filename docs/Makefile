PDF=DroneComm_Report.pdf
TEX=DroneComm_Report.tex

pdf: $(TEX)
	pdflatex -interaction=nonstopmode $(TEX)
	pdflatex -interaction=nonstopmode $(TEX)

clean:
	rm -f *.aux *.log *.out *.toc *.pdf
