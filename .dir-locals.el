((nil . ((indent-tabs-mode . nil)       ; always use spaces for tabs
         (require-final-newline . t)))  ; add final newline on save
 (java-mode . ((whitespace-line-column . 118)))
 (clojure-mode . ((cider-preferred-build-tool . clojure-cli)
                  (cider-clojure-cli-aliases . "dev:user")
                  (cljr-favor-prefix-notation . nil)
                  (cljr-insert-newline-after-require . t)
                  ;; prefer keeping source width about ~118, GitHub seems to cut
                  ;; off stuff at either 119 or 120 and it's nicer to look at
                  ;; code in GH when you don't have to scroll back and forth
                  (fill-column . 118)
                  (whitespace-line-column . 118)
                  (column-enforce-column . 118)
                  (clojure-docstring-fill-column . 118)
                  (clojure-indent-style . always-align)
                  (eval . (put-clojure-indent 'with-meta '(:form)))
                  (eval . (put-clojure-indent 'with-bindings* '(:form)))))
 (markdown-mode . ((fill-column . 80)
                   (column-enforce-column . 80))))
