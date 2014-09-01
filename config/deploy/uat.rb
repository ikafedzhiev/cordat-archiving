set :stage, :uat

set :ssh_options, {
  auth_methods: ["password"],
  user: 'smx',
  password: 'Kon6QuaytUc?',
  port: 8101,
  paranoid: false
}

role :karaf, %w{esb-a-uat.colo.elex.be esb-b-uat.colo.elex.be}
