#!/bin/bash

curl --request POST \
  --url "http://localhost:8080/api/documents/upload" \
  --form "file=@test-document.txt"