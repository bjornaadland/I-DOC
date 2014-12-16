//
// Very simple file receiver and server.
//
package main

import (
	"io"
	"net/http"
	"os"
	"strings"
	"fmt"
	"mime"
)

func newDocHandler(w http.ResponseWriter, req *http.Request) {
	fmt.Printf("%s request received %s\n", req.Method, req.URL.Path)
	if req.Method == "PUT" {
		uuid := strings.Split(req.URL.Path, "/")[3]
		reader, err := req.MultipartReader()
		if err != nil {
			fmt.Printf("reader failed %s\n", err)
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		part, err := reader.NextPart();
		for ; err == nil; part, err = reader.NextPart() {
			v := part.Header.Get("Content-Type")
			d, _, err := mime.ParseMediaType(v)
			if err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
			fmt.Printf("Part with type: %s name: %s\n", d, part.FileName())

			extension := ""
			if d == "text/json" {
				extension = ".metadata"
			} else if d == "image/jpeg" {
				extension = ".jpg"
			}

			outfile, err := os.OpenFile(uuid + extension, os.O_CREATE|os.O_WRONLY, 0666)
			if err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
			io.Copy(outfile, part)
		}
		if err != nil && err != io.EOF {
			fmt.Printf("Error in next part %s ", err)
			w.WriteHeader(http.StatusInternalServerError)
		} else {
			w.WriteHeader(http.StatusOK)
		}

	} else {
		http.Error(w, "invalid method", http.StatusBadRequest)
		return
	}
}

func main() {
	http.HandleFunc("/i-doc/documents/", newDocHandler)
	http.ListenAndServe(":9000", nil)
}
