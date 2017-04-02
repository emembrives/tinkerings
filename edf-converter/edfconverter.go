package main

import (
	"flag"
	"fmt"

	"github.com/ishiikurisu/edf"
)

var (
	inputFile = flag.String("input", "your_file.edf", "Input EDF+ file")
)

func main() {
	flag.Parse()
	edfContents := edf.ReadFile(*inputFile)
	fmt.Println(edfContents.WriteCSV())
}
