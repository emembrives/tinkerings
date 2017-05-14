package main

import (
	"flag"
	"fmt"

	"github.com/emembrives/edf"
)

var (
	inputFile = flag.String("input", "your_file.edf", "Input EDF+ file")
)

func main() {
	flag.Parse()
	edfContents, _ := edf.ReadEDF(*inputFile)
	fmt.Printf("%+v\n", edfContents.Header)
	signal, _ := edfContents.GetSignals()[0]
	aS := signal.(AnnotationSignal)
}
