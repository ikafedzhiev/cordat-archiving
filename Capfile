require 'capistrano/setup'
require 'capistrano-karaf'
require 'capistrano-karaf/install'

include Install
include Capistrano_Karaf

namespace :cordatarch do

    task :release_1_0 do
        on roles(:karaf) do
            upgrade([{ :feature_url => "mvn:com.melexis.repository/cordat-archiving-repo/1.0.1/xml/features",
                       :feature => "finallotshipments",
                       :version => "1.0.1"
                     },
		     { :feature_url => "mvn:com.melexis.repository/cordat-archiving-repo/1.0.1/xml/features",
                       :feature => "cordatarchiving",
                       :version => "1.0.1"
                     }])
        end
    end

    task :uninstall do
        on roles(:karaf) do
            feature_uninstall("finallotshipments","cordatarchiving")
        end
    end
end
