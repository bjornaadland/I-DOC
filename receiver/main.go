//
// Very simple file receiver and server.
//
package main

import (
	"github.com/twinj/uuid"
	"io"
	"net/http"
	"os"
)

func newDocHandler(w http.ResponseWriter, req *http.Request) {
	if req.Method == "POST" {
		u := uuid.NewV4()
		filename := uuid.Formatter(u, uuid.Clean) + ".jpg"
		outfile, err := os.OpenFile(filename, os.O_CREATE|os.O_WRONLY, 0666)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		io.Copy(outfile, req.Body)
		w.Header().Add("Location", filename)
		w.WriteHeader(http.StatusCreated)
	} else {
		http.Error(w, "invalid method", http.StatusInternalServerError)
	}
}

func main() {
	http.HandleFunc("/new-doc", newDocHandler)
	http.ListenAndServe(":9000", nil)
}
